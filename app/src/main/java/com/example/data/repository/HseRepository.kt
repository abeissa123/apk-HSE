package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.HseDao
import com.example.data.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HseRepository(
    private val dao: HseDao,
    val incidentRepository: IncidentReportRepository = IncidentReportRepository(dao)
) {
    companion object {
        private const val TAG = "HseRepository"
        private const val COLLECTION_USERS = "hse_users"
        private const val COLLECTION_CONTENTS = "hse_contents"
        private const val COLLECTION_AUDITS = "hse_audits"
        private const val COLLECTION_SURVEYS = "hse_surveys"
        private const val COLLECTION_STORIES = "hse_stories"
        private const val COLLECTION_NOTIFICATIONS = "hse_notifications"
        private const val COLLECTION_ATTESTATIONS = "hse_attestations"
        private const val COLLECTION_TRAININGS = "hse_trainings"
    }

    private var auditsListener: ListenerRegistration? = null
    private var contentsListener: ListenerRegistration? = null
    private var storiesListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var surveysListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var attestationsListener: ListenerRegistration? = null
    private var trainingsListener: ListenerRegistration? = null

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore not available: ${t.message}")
            null
        }
    }

    val user: Flow<UserEntity?> = dao.getUser()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers().map { users ->
        users.filterNot { AppDatabase.isDemoUserEmail(it.email) }
    }
    val allContents: Flow<List<ContentEntity>> = dao.getAllContents()
    val allAttestations: Flow<List<DocumentAttestationEntity>> = dao.getAllAttestations()
    val certifiedAttestationsCount: Flow<Int> = dao.getCertifiedAttestationsCount()

    fun getUserByEmail(email: String): Flow<UserEntity?> = dao.getUserByEmail(email)

    suspend fun getUserDirect(email: String): UserEntity? = dao.getUserDirect(email)

    suspend fun clearUsers() = dao.clearUsers()

    suspend fun fetchUserFromFirestore(email: String): UserEntity? {
        try {
            val firestore = getFirestore() ?: return null
            val docKey = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
            var doc = firestore.collection(COLLECTION_USERS).document(docKey).get().await()
            if (!doc.exists()) {
                val rawKey = email.trim().replace(".", "_").replace("@", "_at_")
                if (rawKey != docKey) {
                    doc = firestore.collection(COLLECTION_USERS).document(rawKey).get().await()
                }
            }
            if (doc.exists()) {
                val name = doc.getString("name") ?: ""
                val role = doc.getString("role") ?: "Collaborateur"
                val site = doc.getString("site") ?: "Site de Figuil"
                val phone = doc.getString("phone") ?: ""
                val avatarInitials = doc.getString("avatarInitials") ?: ""
                val pushNotificationsEnabled = doc.getBoolean("pushNotificationsEnabled") ?: true
                val mfaEnabled = doc.getBoolean("mfaEnabled") ?: true
                val department = doc.getString("department") ?: "Sécurité Industrielle"
                val rawStatus = doc.getString("status") ?: "Actif"
                val status = if (rawStatus.equals("En attente", ignoreCase = true)) "Actif" else rawStatus
                val isOnline = doc.getBoolean("isOnline") ?: false
                val lastActive = doc.getLong("lastActiveMillis") ?: System.currentTimeMillis()
                val password = doc.getString("password") ?: ""
                val firebaseUid = doc.getString("firebaseUid") ?: ""
                val authSynced = doc.getBoolean("authSynced") ?: true
                val resetRequested = doc.getBoolean("resetRequested") ?: false
                val resetRequestNote = doc.getString("resetRequestNote") ?: ""

                val initials = if (avatarInitials.isNotBlank()) avatarInitials else {
                    name.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { "UT" }
                }

                val existingLocal = dao.getUserDirect(email)
                val userEntity = UserEntity(
                    email = email,
                    name = if (name.isNotBlank()) name else email.substringBefore("@"),
                    role = role,
                    site = site,
                    phone = phone,
                    avatarInitials = initials,
                    pushNotificationsEnabled = pushNotificationsEnabled,
                    mfaEnabled = mfaEnabled,
                    department = department,
                    status = status,
                    isOnline = isOnline,
                    lastActiveMillis = lastActive,
                    password = password.ifBlank { existingLocal?.password ?: "" },
                    firebaseUid = firebaseUid.ifBlank { existingLocal?.firebaseUid ?: "" },
                    authSynced = authSynced,
                    resetRequested = resetRequested,
                    resetRequestNote = resetRequestNote
                )
                dao.insertUser(userEntity)
                return userEntity
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch user from Firestore: ${e.message}")
        }
        return null
    }
    val allIncidents: Flow<List<IncidentEntity>> = incidentRepository.allIncidents
    val pendingRisksCount: Flow<Int> = incidentRepository.pendingRisksCount
    val allTrainings: Flow<List<TrainingEntity>> = dao.getAllTrainings()
    val allEvents: Flow<List<EventEntity>> = dao.getAllEvents()
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications().map { list ->
        list.filterNot { AppDatabase.DEMO_NOTIFICATION_TITLES.contains(it.title) }
    }
    val unreadCount: Flow<Int> = dao.getUnreadNotificationsCount()
    val allSurveys: Flow<List<SurveyEntity>> = dao.getAllSurveys()
    val allSuccessStories: Flow<List<SuccessStoryEntity>> = dao.getAllSuccessStories()
    val allAudits: Flow<List<SafetyAuditEntity>> = dao.getAllAudits()

    fun getProcedureSteps(procedureId: String): Flow<List<ProcedureStepEntity>> =
        dao.getProcedureSteps(procedureId)

    suspend fun insertUser(user: UserEntity) {
        val existing = dao.getUserDirect(user.email)
        val mergedUser = user.copy(
            password = user.password.ifBlank { existing?.password ?: "" },
            firebaseUid = user.firebaseUid.ifBlank { existing?.firebaseUid ?: "" }
        )
        dao.insertUser(mergedUser)
        syncUserToFirestore(mergedUser)
    }

    suspend fun updateUser(user: UserEntity) {
        val existing = dao.getUserDirect(user.email)
        val mergedUser = user.copy(
            password = user.password.ifBlank { existing?.password ?: "" },
            firebaseUid = user.firebaseUid.ifBlank { existing?.firebaseUid ?: "" }
        )
        dao.updateUser(mergedUser)
        syncUserToFirestore(mergedUser)
    }

    suspend fun updateUserPasswordAndSync(
        email: String,
        newPassword: String,
        firebaseUid: String = "",
        authSynced: Boolean = true
    ) {
        dao.updateUserPassword(email, newPassword, authSynced)
        val existing = dao.getUserDirect(email)
        if (existing != null) {
            val updated = existing.copy(
                password = newPassword,
                firebaseUid = firebaseUid.ifBlank { existing.firebaseUid },
                authSynced = authSynced,
                resetRequested = false,
                resetRequestNote = ""
            )
            dao.insertUser(updated)
            syncUserToFirestore(updated)
        } else {
            try {
                val firestore = getFirestore() ?: return
                val docKey = email.trim().replace(".", "_").replace("@", "_at_")
                val data = mutableMapOf<String, Any>(
                    "password" to newPassword,
                    "authSynced" to authSynced,
                    "resetRequested" to false,
                    "resetRequestNote" to ""
                )
                if (firebaseUid.isNotBlank()) {
                    data["firebaseUid"] = firebaseUid
                }
                firestore.collection(COLLECTION_USERS).document(docKey)
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update user password in Firestore: ${e.message}")
            }
        }
    }

    suspend fun requestAccountOrPasswordRecovery(identifier: String, note: String): UserEntity? {
        val query = identifier.trim()
        var matchedUser: UserEntity? = null
        try {
            // 1. Check direct email match first
            if (query.contains("@")) {
                matchedUser = dao.getUserDirect(query) ?: fetchUserFromFirestore(query)
            }
            // 2. Search Firestore users by name, email, or phone if not matched yet
            if (matchedUser == null) {
                val firestore = getFirestore()
                if (firestore != null) {
                    val docs = firestore.collection(COLLECTION_USERS).get().await().documents
                    for (doc in docs) {
                        val docEmail = doc.getString("email") ?: continue
                        val docName = doc.getString("name") ?: ""
                        val docPhone = doc.getString("phone") ?: ""
                        if (docEmail.equals(query, ignoreCase = true) ||
                            docName.contains(query, ignoreCase = true) ||
                            (docPhone.isNotBlank() && docPhone.replace(" ", "").contains(query.replace(" ", ""), ignoreCase = true))
                        ) {
                            matchedUser = fetchUserFromFirestore(docEmail)
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up user for recovery: ${e.message}")
        }

        val nowText = "Demande de récupération (${if (note.isNotBlank()) note else "Mot de passe / Compte oublié"})"
        if (matchedUser != null) {
            val updated = matchedUser.copy(
                resetRequested = true,
                resetRequestNote = nowText
            )
            dao.insertUser(updated)
            syncUserToFirestore(updated)
            broadcastNotification(
                title = "🔑 Récupération Compte / MDP : ${matchedUser.name}",
                message = "L'utilisateur ${matchedUser.name} (${matchedUser.email} • Tél: ${matchedUser.phone.ifBlank { "Non renseigné" }}) a signalé un oubli de compte ou mot de passe. Rendez-vous dans Gestion des Utilisateurs.",
                type = "Urgence"
            )
            return updated
        } else {
            broadcastNotification(
                title = "🔑 Assistance Compte / MDP oublié",
                message = "Demande de récupération d'accès reçue pour l'identifiant \"$query\" ($note). Vérifiez dans Gestion des Utilisateurs.",
                type = "Urgence"
            )
            return null
        }
    }

    suspend fun clearAccountRecoveryRequest(email: String) {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.getUserDirect(cleanEmail)
        if (existing != null) {
            val updated = existing.copy(resetRequested = false, resetRequestNote = "")
            dao.insertUser(updated)
            syncUserToFirestore(updated)
        } else {
            try {
                val firestore = getFirestore() ?: return
                val docKey = cleanEmail.replace(".", "_").replace("@", "_at_")
                firestore.collection(COLLECTION_USERS).document(docKey)
                    .set(mapOf("resetRequested" to false, "resetRequestNote" to ""), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear recovery request in Firestore: ${e.message}")
            }
        }
    }

    suspend fun setUserOnline(email: String, isOnline: Boolean) {
        val now = System.currentTimeMillis()
        dao.updateUserOnlineStatus(email, isOnline, now)
        try {
            val firestore = getFirestore() ?: return
            val docKey = email.replace(".", "_").replace("@", "_at_")
            val data = mapOf(
                "isOnline" to isOnline,
                "lastActiveMillis" to now
            )
            firestore.collection(COLLECTION_USERS).document(docKey)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user online status in Firestore: ${e.message}")
        }
    }

    private suspend fun syncUserToFirestore(user: UserEntity) {
        try {
            val firestore = getFirestore() ?: return
            val data = hashMapOf<String, Any>(
                "name" to user.name,
                "role" to user.role,
                "site" to user.site,
                "phone" to user.phone,
                "email" to user.email,
                "avatarInitials" to user.avatarInitials,
                "pushNotificationsEnabled" to user.pushNotificationsEnabled,
                "mfaEnabled" to user.mfaEnabled,
                "department" to user.department,
                "status" to user.status,
                "isOnline" to user.isOnline,
                "lastActiveMillis" to user.lastActiveMillis,
                "authSynced" to user.authSynced,
                "resetRequested" to user.resetRequested,
                "resetRequestNote" to user.resetRequestNote
            )
            if (user.password.isNotBlank()) {
                data["password"] = user.password
            }
            if (user.firebaseUid.isNotBlank()) {
                data["firebaseUid"] = user.firebaseUid
            }
            val docKey = user.email.replace(".", "_").replace("@", "_at_")
            firestore.collection(COLLECTION_USERS).document(docKey)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync user to Firestore: ${e.message}")
        }
    }

    suspend fun reportIncident(incident: IncidentEntity): Long =
        incidentRepository.reportIncident(incident)

    suspend fun updateIncidentStatus(incidentId: Long, status: String) =
        incidentRepository.updateIncidentStatus(incidentId, status)

    suspend fun updateStepCompletion(stepId: Long, isCompleted: Boolean) =
        dao.updateStepCompletion(stepId, isCompleted)

    suspend fun resetProcedureSteps(procedureId: String) =
        dao.resetProcedureSteps(procedureId)

    suspend fun toggleTrainingEnrollment(trainingId: String, isCurrentlyEnrolled: Boolean) {
        val delta = if (isCurrentlyEnrolled) -1 else 1
        dao.updateEnrollment(trainingId, !isCurrentlyEnrolled, delta)
        try {
            val firestore = getFirestore() ?: return
            val docRef = firestore.collection(COLLECTION_TRAININGS).document(trainingId)
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                val currentCount = snap.getLong("enrolledCount")?.toInt() ?: 0
                val newCount = (currentCount + delta).coerceAtLeast(0)
                tx.set(docRef, mapOf("id" to trainingId, "enrolledCount" to newCount), SetOptions.merge())
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync training enrollment to Firestore: ${e.message}")
        }
    }

    suspend fun markNotificationAsRead(id: Long) = dao.markNotificationAsRead(id)
    suspend fun markAllNotificationsAsRead() = dao.markAllNotificationsAsRead()

    suspend fun completeSurvey(surveyId: String) {
        dao.completeSurvey(surveyId)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_SURVEYS).document(surveyId)
                .set(mapOf("isCompleted" to true), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync survey to Firestore: ${e.message}")
        }
    }

    suspend fun toggleStoryLike(storyId: String, isCurrentlyLiked: Boolean) {
        val delta = if (isCurrentlyLiked) -1 else 1
        dao.updateStoryLike(storyId, !isCurrentlyLiked, delta)
        try {
            val firestore = getFirestore() ?: return
            val docRef = firestore.collection(COLLECTION_STORIES).document(storyId)
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    val currentLikes = snap.getLong("likesCount")?.toInt() ?: 0
                    val newLikes = (currentLikes + delta).coerceAtLeast(0)
                    tx.update(docRef, mapOf("likesCount" to newLikes, "isLiked" to !isCurrentlyLiked))
                }
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync story like to Firestore: ${e.message}")
        }
    }

    suspend fun saveSafetyAudit(audit: SafetyAuditEntity) {
        dao.insertAudit(audit)
        try {
            val firestore = getFirestore() ?: return
            val data = hashMapOf<String, Any>(
                "site" to audit.site,
                "auditorName" to audit.auditorName,
                "dateText" to audit.dateText,
                "scorePercentage" to audit.scorePercentage,
                "status" to audit.status,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_AUDITS).add(data).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync audit to Firestore: ${e.message}")
        }
    }

    // Content Management (PDF, Photos, Videos, Links)
    suspend fun addContent(content: ContentEntity) {
        dao.insertContent(content)
        syncContentToFirestore(content)
    }

    suspend fun updateContent(content: ContentEntity) {
        dao.updateContent(content)
        syncContentToFirestore(content)
    }

    suspend fun deleteContent(contentId: String) {
        dao.deleteContent(contentId)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_CONTENTS).document(contentId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete content from Firestore: ${e.message}")
        }
    }

    suspend fun incrementContentViews(contentId: String) {
        dao.incrementContentViews(contentId)
        try {
            val firestore = getFirestore() ?: return
            val docRef = firestore.collection(COLLECTION_CONTENTS).document(contentId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentViews = snapshot.getLong("viewsCount") ?: 0
                    transaction.update(docRef, "viewsCount", currentViews + 1)
                }
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to increment content views in Firestore: ${e.message}")
        }
    }

    // IA Session & Message Operations
    fun getAllIaSessions(): Flow<List<HseIaSessionEntity>> = dao.getAllIaSessions()

    suspend fun insertIaSession(session: HseIaSessionEntity) = dao.insertIaSession(session)

    suspend fun deleteIaSession(sessionId: String) {
        dao.deleteIaMessagesForSession(sessionId)
        dao.deleteIaSession(sessionId)
    }

    fun getIaMessagesForSession(sessionId: String): Flow<List<HseIaMessageEntity>> =
        dao.getIaMessagesForSession(sessionId)

    suspend fun insertIaMessage(message: HseIaMessageEntity): Long = dao.insertIaMessage(message)

    suspend fun clearIaMessagesForSession(sessionId: String) = dao.deleteIaMessagesForSession(sessionId)


    private suspend fun syncContentToFirestore(content: ContentEntity) {
        try {
            val firestore = getFirestore() ?: return
            val data = hashMapOf<String, Any>(
                "id" to content.id,
                "title" to content.title,
                "description" to content.description,
                "type" to content.type,
                "category" to content.category,
                "fileUrl" to content.fileUrl,
                "viewsCount" to content.viewsCount,
                "dateText" to content.dateText,
                "referenceCode" to content.referenceCode,
                "version" to content.version,
                "authorName" to content.authorName,
                "authorRole" to content.authorRole,
                "site" to content.site,
                "timestampMillis" to content.timestampMillis
            )
            firestore.collection(COLLECTION_CONTENTS).document(content.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync content to Firestore: ${e.message}")
        }
    }

    // User & Access Management
    suspend fun addUser(user: UserEntity) {
        dao.insertUser(user)
        syncUserToFirestore(user)
    }

    suspend fun updateUserRoleAndDetails(email: String, role: String, site: String, department: String) {
        val existing = dao.getUserDirect(email)
        if (existing?.isProtectedAdmin == true && !role.contains("Admin", ignoreCase = true)) {
            Log.w(TAG, "Cannot modify protected admin role")
            return
        }
        dao.updateUserRoleAndDetails(email, role, site, department)
        val u = dao.getUserDirect(email)
        if (u != null) {
            syncUserToFirestore(u)
        }
    }

    suspend fun updateUserStatus(email: String, status: String) {
        val existing = dao.getUserDirect(email)
        if (existing?.isProtectedAdmin == true && !status.equals("Actif", ignoreCase = true)) {
            Log.w(TAG, "Cannot deactivate or block protected admin")
            return
        }
        dao.updateUserStatus(email, status)
        val u = dao.getUserDirect(email)
        if (u != null) {
            syncUserToFirestore(u)
        }
    }

    suspend fun deleteUser(email: String) {
        val existing = dao.getUserDirect(email)
        if (existing?.isProtectedAdmin == true) {
            Log.w(TAG, "Cannot delete protected admin")
            return
        }
        dao.deleteUser(email)
        try {
            val firestore = getFirestore() ?: return
            val docKey = email.replace(".", "_").replace("@", "_at_")
            firestore.collection(COLLECTION_USERS).document(docKey).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete user from Firestore: ${e.message}")
        }
    }

    // Success Stories Management
    suspend fun saveSuccessStory(story: SuccessStoryEntity) {
        dao.insertSuccessStory(story)
        try {
            val firestore = getFirestore() ?: return
            val data = hashMapOf<String, Any>(
                "id" to story.id,
                "title" to story.title,
                "description" to story.description,
                "authorName" to story.authorName,
                "authorRole" to story.authorRole,
                "site" to story.site,
                "dateText" to story.dateText,
                "likesCount" to story.likesCount,
                "type" to story.type,
                "isLiked" to story.isLiked,
                "mediaUrl" to story.mediaUrl,
                "durationText" to story.durationText,
                "resolution" to story.resolution
            )
            firestore.collection(COLLECTION_STORIES).document(story.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync success story to Firestore: ${e.message}")
        }
    }

    suspend fun deleteSuccessStory(storyId: String) {
        dao.deleteSuccessStory(storyId)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_STORIES).document(storyId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete success story from Firestore: ${e.message}")
        }
    }

    // Surveys & Inquiries Management
    suspend fun saveSurvey(survey: SurveyEntity) {
        dao.insertSurvey(survey)
        try {
            val firestore = getFirestore() ?: return
            val data = hashMapOf<String, Any>(
                "id" to survey.id,
                "title" to survey.title,
                "description" to survey.description,
                "quarter" to survey.quarter,
                "questionsCount" to survey.questionsCount,
                "status" to survey.status,
                "isCompleted" to survey.isCompleted
            )
            firestore.collection(COLLECTION_SURVEYS).document(survey.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync survey to Firestore: ${e.message}")
        }
    }

    suspend fun deleteSurvey(surveyId: String) {
        dao.deleteSurvey(surveyId)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_SURVEYS).document(surveyId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete survey from Firestore: ${e.message}")
        }
    }

    // Notification Deletion
    suspend fun deleteNotification(notificationId: Long) {
        dao.deleteNotification(notificationId)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("timestampMillis", notificationId)
                .get().await().documents.forEach { doc ->
                    doc.reference.delete()
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete notification from Firestore: ${e.message}")
        }
    }

    suspend fun clearAllNotifications() {
        dao.clearAllNotifications()
        try {
            val firestore = getFirestore() ?: return
            val docs = firestore.collection(COLLECTION_NOTIFICATIONS).get().await().documents
            for (doc in docs) {
                doc.reference.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear notifications from Firestore: ${e.message}")
        }
    }

    // Document Reading Attestations & Certification
    suspend fun recordDocumentAttestation(attestation: DocumentAttestationEntity): Long {
        val uniqueId = if (attestation.id > 1000000L) attestation.id else System.currentTimeMillis()
        val updated = attestation.copy(id = uniqueId, timestampMillis = uniqueId)
        dao.insertAttestation(updated)
        try {
            val firestore = getFirestore()
            val data = hashMapOf<String, Any>(
                "id" to updated.id,
                "documentId" to updated.documentId,
                "documentTitle" to updated.documentTitle,
                "documentReference" to updated.documentReference,
                "userEmail" to updated.userEmail,
                "userName" to updated.userName,
                "userRole" to updated.userRole,
                "userSite" to updated.userSite,
                "timestampMillis" to updated.timestampMillis,
                "timestampText" to updated.timestampText,
                "isCertified" to updated.isCertified,
                "certificateId" to updated.certificateId
            )
            firestore?.collection(COLLECTION_ATTESTATIONS)?.document("att_${updated.id}")
                ?.set(data, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync attestation to Firestore: ${e.message}")
        }
        return uniqueId
    }

    suspend fun deleteAttestation(id: Long) {
        dao.deleteAttestation(id)
        try {
            val firestore = getFirestore() ?: return
            firestore.collection(COLLECTION_ATTESTATIONS).document("att_$id").delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete attestation from Firestore: ${e.message}")
        }
    }

    // Incident Admin Management (Synchronized with Firestore)
    suspend fun deleteIncident(incidentId: Long) {
        incidentRepository.deleteIncident(incidentId)
    }

    // Broadcast Global Notifications & Emergency Alerts
    suspend fun broadcastNotification(title: String, message: String, type: String = "Information") {
        val now = System.currentTimeMillis()
        val notification = NotificationEntity(
            id = now,
            title = title,
            message = message,
            type = type,
            timestampText = "À l'instant",
            timestampMillis = now,
            isRead = false
        )
        dao.insertNotification(notification)
        try {
            val firestore = getFirestore() ?: return
            val data = hashMapOf<String, Any>(
                "title" to title,
                "message" to message,
                "type" to type,
                "timestampMillis" to notification.timestampMillis,
                "timestampText" to notification.timestampText,
                "isRead" to false
            )
            firestore.collection(COLLECTION_NOTIFICATIONS).document("notif_$now")
                .set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync notification to Firestore: ${e.message}")
        }
    }

    suspend fun sendApprovalNotification(userEmail: String, userName: String) {
        val title = "Compte actif !"
        val message = "Le compte de $userName ($userEmail) est actif et synchronisé sur Firebase Auth."
        broadcastNotification(title = title, message = message, type = "Succès")
    }

    fun startRealtimeSync(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        incidentRepository.startRealtimeFirestoreSync(scope)

        // Real-time Contents listener
        try {
            val firestore = getFirestore() ?: return
            contentsListener = firestore.collection(COLLECTION_CONTENTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Contents listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val title = doc.getString("title") ?: continue
                                    val desc = doc.getString("description") ?: ""
                                    val type = doc.getString("type") ?: "PDF"
                                    val category = doc.getString("category") ?: "HSE"
                                    val fileUrl = doc.getString("fileUrl") ?: ""
                                    val views = doc.getLong("viewsCount")?.toInt() ?: 0
                                    val dateText = doc.getString("dateText") ?: "Récemment"
                                    val refCode = doc.getString("referenceCode") ?: ""
                                    val version = doc.getString("version") ?: "v1.0"
                                    val authorName = doc.getString("authorName") ?: "Direction"
                                    val authorRole = doc.getString("authorRole") ?: "Administrateur"
                                    val site = doc.getString("site") ?: "Tous sites"
                                    val ts = doc.getLong("timestampMillis") ?: System.currentTimeMillis()

                                    val content = ContentEntity(
                                        id = id,
                                        title = title,
                                        description = desc,
                                        type = type,
                                        category = category,
                                        fileUrl = fileUrl,
                                        viewsCount = views,
                                        dateText = dateText,
                                        referenceCode = refCode,
                                        version = version,
                                        authorName = authorName,
                                        authorRole = authorRole,
                                        site = site,
                                        timestampMillis = ts
                                    )
                                    scope.launch { dao.insertContent(content) }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    scope.launch { dao.deleteContent(id) }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start contents realtime sync: ${e.message}")
        }

        // Real-time Users & Online Presence & Account Recovery listener
        try {
            val firestore = getFirestore() ?: return
            usersListener = firestore.collection(COLLECTION_USERS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Users listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val email = doc.getString("email") ?: continue
                            if (AppDatabase.isDemoUserEmail(email)) {
                                scope.launch {
                                    dao.deleteUser(email)
                                    try { doc.reference.delete() } catch (_: Exception) {}
                                }
                                continue
                            }
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("name") ?: email.substringBefore("@")
                                    val role = doc.getString("role") ?: "Collaborateur"
                                    val site = doc.getString("site") ?: "Site Figuil"
                                    val phone = doc.getString("phone") ?: ""
                                    val initials = doc.getString("avatarInitials") ?: "UT"
                                    val push = doc.getBoolean("pushNotificationsEnabled") ?: true
                                    val mfa = doc.getBoolean("mfaEnabled") ?: true
                                    val dept = doc.getString("department") ?: "Sécurité Industrielle"
                                    val rawStatus = doc.getString("status") ?: "Actif"
                                    val status = if (rawStatus.equals("En attente", ignoreCase = true)) "Actif" else rawStatus
                                    val isOnline = doc.getBoolean("isOnline") ?: false
                                    val lastActive = doc.getLong("lastActiveMillis") ?: System.currentTimeMillis()
                                    val password = doc.getString("password") ?: ""
                                    val firebaseUid = doc.getString("firebaseUid") ?: ""
                                    val authSynced = doc.getBoolean("authSynced") ?: true
                                    val resetRequested = doc.getBoolean("resetRequested") ?: false
                                    val resetRequestNote = doc.getString("resetRequestNote") ?: ""

                                    scope.launch {
                                        val existingLocal = dao.getUserDirect(email)
                                        val user = UserEntity(
                                            email = email,
                                            name = name,
                                            role = role,
                                            site = site,
                                            phone = phone,
                                            avatarInitials = initials,
                                            pushNotificationsEnabled = push,
                                            mfaEnabled = mfa,
                                            department = dept,
                                            status = status,
                                            isOnline = isOnline,
                                            lastActiveMillis = lastActive,
                                            password = password.ifBlank { existingLocal?.password ?: "" },
                                            firebaseUid = firebaseUid.ifBlank { existingLocal?.firebaseUid ?: "" },
                                            authSynced = authSynced,
                                            resetRequested = resetRequested,
                                            resetRequestNote = resetRequestNote
                                        )
                                        dao.insertUser(user)
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    scope.launch { dao.deleteUser(email) }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start users realtime sync: ${e.message}")
        }

        // Real-time Success Stories listener
        try {
            val firestore = getFirestore() ?: return
            storiesListener = firestore.collection(COLLECTION_STORIES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Stories listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val story = SuccessStoryEntity(
                                        id = id,
                                        title = doc.getString("title") ?: "",
                                        description = doc.getString("description") ?: "",
                                        authorName = doc.getString("authorName") ?: doc.getString("author") ?: "Collaborateur",
                                        authorRole = doc.getString("authorRole") ?: "Opérateur",
                                        site = doc.getString("site") ?: "Tous sites",
                                        dateText = doc.getString("dateText") ?: "Récemment",
                                        likesCount = doc.getLong("likesCount")?.toInt() ?: 0,
                                        isLiked = doc.getBoolean("isLiked") ?: false,
                                        type = doc.getString("type") ?: "TEMOIGNAGE",
                                        mediaUrl = doc.getString("mediaUrl") ?: "",
                                        durationText = doc.getString("durationText") ?: "",
                                        resolution = doc.getString("resolution") ?: ""
                                    )
                                    scope.launch { dao.insertSuccessStory(story) }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    scope.launch { dao.deleteSuccessStory(id) }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start stories realtime sync: ${e.message}")
        }

        // Real-time Surveys listener
        try {
            val firestore = getFirestore() ?: return
            surveysListener = firestore.collection(COLLECTION_SURVEYS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Surveys listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val id = doc.getString("id") ?: doc.id
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val survey = SurveyEntity(
                                        id = id,
                                        title = doc.getString("title") ?: "",
                                        description = doc.getString("description") ?: "",
                                        quarter = doc.getString("quarter") ?: "T1 2026",
                                        questionsCount = doc.getLong("questionsCount")?.toInt() ?: 5,
                                        status = doc.getString("status") ?: "Disponible",
                                        isCompleted = doc.getBoolean("isCompleted") ?: false
                                    )
                                    scope.launch { dao.insertSurvey(survey) }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    scope.launch { dao.deleteSurvey(id) }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start surveys realtime sync: ${e.message}")
        }

        // Real-time Notifications listener
        try {
            val firestore = getFirestore() ?: return
            notificationsListener = firestore.collection(COLLECTION_NOTIFICATIONS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Notifications listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val ts = doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                            val title = doc.getString("title") ?: ""
                            if (AppDatabase.DEMO_NOTIFICATION_TITLES.contains(title)) {
                                scope.launch {
                                    dao.deleteNotification(ts)
                                    try { doc.reference.delete() } catch (_: Exception) {}
                                }
                                continue
                            }
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val notif = NotificationEntity(
                                        id = ts,
                                        title = title,
                                        message = doc.getString("message") ?: "",
                                        type = doc.getString("type") ?: "Information",
                                        timestampText = doc.getString("timestampText") ?: "Récemment",
                                        timestampMillis = ts,
                                        isRead = doc.getBoolean("isRead") ?: false
                                    )
                                    scope.launch { dao.insertNotification(notif) }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    scope.launch { dao.deleteNotification(ts) }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start notifications realtime sync: ${e.message}")
        }

        // Real-time Document Attestations listener
        try {
            val firestore = getFirestore() ?: return
            attestationsListener = firestore.collection(COLLECTION_ATTESTATIONS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Attestations listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val id = doc.getLong("id") ?: doc.id.replace("att_", "").toLongOrNull() ?: continue
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val att = DocumentAttestationEntity(
                                        id = id,
                                        documentId = doc.getString("documentId") ?: "",
                                        documentTitle = doc.getString("documentTitle") ?: "",
                                        documentReference = doc.getString("documentReference") ?: "",
                                        userEmail = doc.getString("userEmail") ?: "",
                                        userName = doc.getString("userName") ?: "",
                                        userRole = doc.getString("userRole") ?: "Collaborateur",
                                        userSite = doc.getString("userSite") ?: "Site Figuil",
                                        timestampMillis = doc.getLong("timestampMillis") ?: id,
                                        timestampText = doc.getString("timestampText") ?: "Aujourd'hui",
                                        isCertified = doc.getBoolean("isCertified") ?: true,
                                        certificateId = doc.getString("certificateId") ?: ""
                                    )
                                    scope.launch { dao.insertAttestation(att) }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    scope.launch { dao.deleteAttestation(id) }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start attestations realtime sync: ${e.message}")
        }

        // Real-time Audits listener
        try {
            val firestore = getFirestore() ?: return
            auditsListener = firestore.collection(COLLECTION_AUDITS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Audits listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        val audits = snapshots.documents.mapNotNull { doc ->
                            try {
                                SafetyAuditEntity(
                                    site = doc.getString("site") ?: "Site Figuil",
                                    auditorName = doc.getString("auditorName") ?: "Auditeur",
                                    dateText = doc.getString("dateText") ?: "Récemment",
                                    scorePercentage = doc.getLong("scorePercentage")?.toInt() ?: 85,
                                    status = doc.getString("status") ?: "Conforme"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (audits.isNotEmpty()) {
                            scope.launch {
                                audits.forEach { dao.insertAudit(it) }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start audits realtime sync: ${e.message}")
        }
    }

    fun stopRealtimeSync() {
        incidentRepository.stopRealtimeFirestoreSync()
        auditsListener?.remove()
        auditsListener = null
        contentsListener?.remove()
        contentsListener = null
        storiesListener?.remove()
        storiesListener = null
        usersListener?.remove()
        usersListener = null
        surveysListener?.remove()
        surveysListener = null
        notificationsListener?.remove()
        notificationsListener = null
        attestationsListener?.remove()
        attestationsListener = null
        trainingsListener?.remove()
        trainingsListener = null
    }
}
