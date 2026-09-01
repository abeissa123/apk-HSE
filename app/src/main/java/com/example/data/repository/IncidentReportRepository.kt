package com.example.data.repository

import android.util.Log
import com.example.data.db.HseDao
import com.example.data.entity.IncidentEntity
import com.example.data.entity.NotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
     * Local Room single source of truth for UI observation
     */
    val allIncidents: Flow<List<IncidentEntity>> = dao.getAllIncidents()
    val pendingRisksCount: Flow<Int> = dao.getPendingRisksCount()

    /**
     * Creates an incident report:
     * 1. Saves locally in Room first for immediate offline-first UI responsiveness.
     * 2. Automatically posts a safety notification in Room.
     * 3. Syncs to Firebase Firestore in the background.
     */
    suspend fun reportIncident(incident: IncidentEntity): Long {
        // 1. Insert into local Room database
        val generatedId = dao.insertIncident(incident)
        val updatedIncident = incident.copy(id = generatedId)

        // 2. Post corresponding local notification
        dao.insertNotification(
            NotificationEntity(
                title = "Nouvel incident : ${incident.title}",
                message = "Signalé par ${incident.reportedBy} sur le ${incident.site} (${incident.location}). Gravité: ${incident.severity}.",
                type = if (incident.severity == "Critique") "Urgence" else "Sécurité",
                timestampText = "À l'instant",
                timestampMillis = System.currentTimeMillis(),
                isRead = false
            )
        )

        // 3. Sync to Firebase Firestore
        syncIncidentToFirestore(updatedIncident)

        return generatedId
    }

    /**
     * Updates the status of an incident report in Room and Firestore.
     */
    suspend fun updateIncidentStatus(incidentId: Long, status: String) {
        dao.updateIncidentStatus(incidentId, status)
        try {
            val firestore = getFirestore() ?: return
            val docRef = firestore.collection(COLLECTION_INCIDENTS).document("incident_$incidentId")
            docRef.update("status", status).await()
            Log.d(TAG, "Successfully updated incident $incidentId status to $status in Firestore")
        } catch (t: Throwable) {
            Log.w(TAG, "Could not update status in Firestore for incident $incidentId: ${t.message}")
        }
    }

    /**
     * Pushes a single incident entity to Firestore.
     */
    suspend fun syncIncidentToFirestore(incident: IncidentEntity) {
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
        try {
            val firestore = getFirestore() ?: return
            realtimeListener = firestore.collection(COLLECTION_INCIDENTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val remoteIncidents = snapshots.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getLong("id") ?: doc.id.replace("incident_", "").toLongOrNull() ?: 0L
                                val title = doc.getString("title") ?: return@mapNotNull null
                                val category = doc.getString("category") ?: "Sécurité"
                                val severity = doc.getString("severity") ?: "Majeur"
                                val site = doc.getString("site") ?: "Site Figuil"
                                val location = doc.getString("location") ?: "Atelier"
                                val description = doc.getString("description") ?: ""
                                val reportedBy = doc.getString("reportedBy") ?: "Collaborateur"
                                val timestampText = doc.getString("timestampText") ?: "Récemment"
                                val timestampMillis = doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                                val status = doc.getString("status") ?: "En cours"

                                IncidentEntity(
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
                            } catch (t: Throwable) {
                                Log.e(TAG, "Error parsing Firestore incident doc ${doc.id}: ${t.message}")
                                null
                            }
                        }

                        if (remoteIncidents.isNotEmpty()) {
                            scope.launch {
                                dao.insertIncidents(remoteIncidents)
                                Log.d(TAG, "Synced ${remoteIncidents.size} incidents from Firestore into Room")
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
