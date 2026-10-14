package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.HseDao
import com.example.data.entity.IncidentEntity
import com.example.data.entity.NotificationEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository layer that coordinates local Room database persistence
 * and remote Firebase Firestore cloud synchronization for HSE Incident Reports.
 */
class IncidentReportRepository(
    private val dao: HseDao
) {
    companion object {
        private const val TAG = "IncidentReportRepo"
        private const val COLLECTION_INCIDENTS = "hse_incidents"
        private const val COLLECTION_NOTIFICATIONS = "hse_notifications"
    }

    private var realtimeListener: ListenerRegistration? = null

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore not available: ${t.message}")
            null
        }
    }

    /**
     * Local Room single source of truth for UI observation (filtered to exclude any demo incidents)
     */
    val allIncidents: Flow<List<IncidentEntity>> = dao.getAllIncidents().map { list ->
        list.filterNot { AppDatabase.isDemoIncident(it.title, it.reportedBy) }
    }
    val pendingRisksCount: Flow<Int> = dao.getPendingRisksCount()

    /**
     * Creates an incident report:
     * 1. Assigns a globally unique timestamp ID so multi-user devices never collide.
     * 2. Saves locally in Room first for immediate UI responsiveness.
     * 3. Posts a safety notification in Room and broadcasts to Firestore for all users.
     * 4. Syncs the incident to Firebase Firestore in real time.
     */
    suspend fun reportIncident(incident: IncidentEntity): Long {
        val now = System.currentTimeMillis()
        val uniqueId = if (incident.id > 1000000L) incident.id else now
        val updatedIncident = incident.copy(
            id = uniqueId,
            timestampMillis = if (incident.timestampMillis > 0) incident.timestampMillis else now
        )

        // 1. Insert into local Room database
        dao.insertIncident(updatedIncident)

        // 2. Post corresponding notification locally and on Firestore for all users in real time
        val notifTitle = "⚠ Incident signalé : ${updatedIncident.title}"
        val notifMessage = "Signalé par ${updatedIncident.reportedBy} sur ${updatedIncident.site} (${updatedIncident.location}). Gravité : ${updatedIncident.severity}."
        val notifType = if (updatedIncident.severity.equals("Critique", ignoreCase = true)) "Urgence" else "Sécurité"
        val notif = NotificationEntity(
            id = now,
            title = notifTitle,
            message = notifMessage,
            type = notifType,
            timestampText = "À l'instant",
            timestampMillis = now,
            isRead = false
        )
        dao.insertNotification(notif)
        try {
            val firestore = getFirestore()
            val notifData = hashMapOf<String, Any>(
                "title" to notifTitle,
                "message" to notifMessage,
                "type" to notifType,
                "timestampMillis" to now,
                "timestampText" to "À l'instant",
                "isRead" to false
            )
            firestore?.collection(COLLECTION_NOTIFICATIONS)?.document("notif_$now")?.set(notifData, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync incident notification to Firestore: ${e.message}")
        }

        // 3. Sync to Firebase Firestore
        syncIncidentToFirestore(updatedIncident)

        return uniqueId
    }

    /**
     * Updates the status of an incident report in Room and Firestore across all user interfaces.
     */
    suspend fun updateIncidentStatus(incidentId: Long, status: String) {
        dao.updateIncidentStatus(incidentId, status)
        try {
            val firestore = getFirestore() ?: return
            val docRef = firestore.collection(COLLECTION_INCIDENTS).document("incident_$incidentId")
            docRef.set(mapOf("status" to status), SetOptions.merge()).await()
            Log.d(TAG, "Successfully updated incident $incidentId status to $status in Firestore")
        } catch (t: Throwable) {
            Log.w(TAG, "Could not update status in Firestore for incident $incidentId: ${t.message}")
        }
    }

    /**
     * Deletes an incident from Room and Firestore so it is removed from all user interfaces in real time.
     */
    suspend fun deleteIncident(incidentId: Long) {
        dao.deleteIncident(incidentId)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_INCIDENTS).document("incident_$incidentId").delete().await()
        } catch (t: Throwable) {
            Log.w(TAG, "Could not delete incident $incidentId from Firestore: ${t.message}")
        }
    }

    /**
     * Pushes a single incident entity to Firestore.
     */
    suspend fun syncIncidentToFirestore(incident: IncidentEntity) {
        if (AppDatabase.isDemoIncident(incident.title, incident.reportedBy)) return
        try {
            val firestore = getFirestore() ?: return
            val docId = "incident_${incident.id}"
            val data = hashMapOf<String, Any>(
                "id" to incident.id,
                "title" to incident.title,
                "category" to incident.category,
                "severity" to incident.severity,
                "site" to incident.site,
                "location" to incident.location,
                "description" to incident.description,
                "reportedBy" to incident.reportedBy,
                "timestampText" to incident.timestampText,
                "timestampMillis" to incident.timestampMillis,
                "status" to incident.status
            )

            firestore.collection(COLLECTION_INCIDENTS)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced incident ${incident.id} to Firestore")
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore sync skipped or failed for incident ${incident.id}: ${t.message}")
        }
    }

    /**
     * Starts listening to real-time updates from Firestore collection and syncing back to Room.
     */
    fun startRealtimeFirestoreSync(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        stopRealtimeFirestoreSync()
        scope.launch {
            try {
                dao.deleteIncidentsByTitles(AppDatabase.DEMO_INCIDENT_TITLES.toList())
            } catch (_: Exception) {
            }
        }
        try {
            val firestore = getFirestore() ?: return
            realtimeListener = firestore.collection(COLLECTION_INCIDENTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val id = doc.getLong("id") ?: doc.id.replace("incident_", "").toLongOrNull() ?: continue
                            val title = doc.getString("title") ?: ""
                            val reportedBy = doc.getString("reportedBy") ?: "Collaborateur"

                            // Automatically purge any demo incident from Firestore and Room
                            if (AppDatabase.isDemoIncident(title, reportedBy)) {
                                scope.launch {
                                    dao.deleteIncident(id)
                                    try { doc.reference.delete() } catch (_: Exception) {}
                                }
                                continue
                            }

                            when (change.type) {
                                DocumentChange.Type.ADDED,
                                DocumentChange.Type.MODIFIED -> {
                                    if (title.isBlank()) continue
                                    val category = doc.getString("category") ?: "Sécurité"
                                    val severity = doc.getString("severity") ?: "Élevé"
                                    val site = doc.getString("site") ?: "Site Figuil"
                                    val location = doc.getString("location") ?: "Atelier"
                                    val description = doc.getString("description") ?: ""
                                    val timestampText = doc.getString("timestampText") ?: "Récemment"
                                    val timestampMillis = doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                                    val status = doc.getString("status") ?: "En cours"

                                    val entity = IncidentEntity(
                                        id = id,
                                        title = title,
                                        category = category,
                                        severity = severity,
                                        site = site,
                                        location = location,
                                        description = description,
                                        reportedBy = reportedBy,
                                        timestampText = timestampText,
                                        timestampMillis = timestampMillis,
                                        status = status
                                    )
                                    scope.launch {
                                        dao.insertIncident(entity)
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    scope.launch {
                                        dao.deleteIncident(id)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not start Firestore real-time listener: ${t.message}")
        }
    }

    /**
     * Stops the real-time listener when no longer needed.
     */
    fun stopRealtimeFirestoreSync() {
        realtimeListener?.remove()
        realtimeListener = null
    }
}
