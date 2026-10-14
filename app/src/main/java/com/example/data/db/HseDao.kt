package com.example.data.db

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HseDao {
    // Users
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserDirect(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE email = :email")
    suspend fun updateUserStatus(email: String, status: String)

    @Query("UPDATE users SET role = :role, site = :site, department = :department WHERE email = :email")
    suspend fun updateUserRoleAndDetails(email: String, role: String, site: String, department: String)

    @Query("UPDATE users SET isOnline = :isOnline, lastActiveMillis = :lastActiveMillis WHERE email = :email")
    suspend fun updateUserOnlineStatus(email: String, isOnline: Boolean, lastActiveMillis: Long)

    @Query("UPDATE users SET password = :password, authSynced = :authSynced, resetRequested = 0, resetRequestNote = '' WHERE email = :email")
    suspend fun updateUserPassword(email: String, password: String, authSynced: Boolean = true)

    @Query("UPDATE users SET resetRequested = :requested, resetRequestNote = :note WHERE email = :email")
    suspend fun updateUserResetRequest(email: String, requested: Boolean, note: String)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUser(email: String)

    @Query("DELETE FROM users WHERE email IN (:emails)")
    suspend fun deleteUsersByEmails(emails: List<String>)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // Multimedia Contents (PDFs, Photos, Videos, Links, Testimonials)
    @Query("SELECT * FROM contents ORDER BY timestampMillis DESC")
    fun getAllContents(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM contents WHERE type = :type ORDER BY timestampMillis DESC")
    fun getContentsByType(type: String): Flow<List<ContentEntity>>

    @Query("SELECT * FROM contents WHERE category = :category ORDER BY timestampMillis DESC")
    fun getContentsByCategory(category: String): Flow<List<ContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: ContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(contents: List<ContentEntity>)

    @Update
    suspend fun updateContent(content: ContentEntity)

    @Query("DELETE FROM contents WHERE id = :contentId")
    suspend fun deleteContent(contentId: String)

    @Query("UPDATE contents SET viewsCount = viewsCount + 1 WHERE id = :contentId")
    suspend fun incrementContentViews(contentId: String)

    // Document Reading Attestations & Compliance Certifications
    @Query("SELECT * FROM document_attestations ORDER BY timestampMillis DESC")
    fun getAllAttestations(): Flow<List<DocumentAttestationEntity>>

    @Query("SELECT * FROM document_attestations WHERE userEmail = :userEmail ORDER BY timestampMillis DESC")
    fun getAttestationsByUser(userEmail: String): Flow<List<DocumentAttestationEntity>>

    @Query("SELECT * FROM document_attestations WHERE documentId = :documentId ORDER BY timestampMillis DESC")
    fun getAttestationsByDocument(documentId: String): Flow<List<DocumentAttestationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttestation(attestation: DocumentAttestationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttestations(attestations: List<DocumentAttestationEntity>)

    @Query("DELETE FROM document_attestations WHERE id = :attestationId")
    suspend fun deleteAttestation(attestationId: Long)

    @Query("SELECT COUNT(*) FROM document_attestations WHERE isCertified = 1")
    fun getCertifiedAttestationsCount(): Flow<Int>

    // Incidents
    @Query("SELECT * FROM incidents ORDER BY timestampMillis DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<IncidentEntity>)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("UPDATE incidents SET status = :newStatus WHERE id = :incidentId")
    suspend fun updateIncidentStatus(incidentId: Long, newStatus: String)

    @Query("DELETE FROM incidents WHERE id = :incidentId")
    suspend fun deleteIncident(incidentId: Long)

    @Query("DELETE FROM incidents WHERE title IN (:titles)")
    suspend fun deleteIncidentsByTitles(titles: List<String>)

    @Query("SELECT COUNT(*) FROM incidents WHERE status = 'En cours'")
    fun getPendingRisksCount(): Flow<Int>

    // Procedure Steps
    @Query("SELECT * FROM procedure_steps WHERE procedureId = :procedureId ORDER BY stepNumber ASC")
    fun getProcedureSteps(procedureId: String): Flow<List<ProcedureStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedureSteps(steps: List<ProcedureStepEntity>)

    @Query("UPDATE procedure_steps SET isCompleted = :isCompleted WHERE id = :stepId")
    suspend fun updateStepCompletion(stepId: Long, isCompleted: Boolean)

    @Query("UPDATE procedure_steps SET isCompleted = 0 WHERE procedureId = :procedureId")
    suspend fun resetProcedureSteps(procedureId: String)

    // Trainings
    @Query("SELECT * FROM trainings ORDER BY id ASC")
    fun getAllTrainings(): Flow<List<TrainingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainings(trainings: List<TrainingEntity>)

    @Query("UPDATE trainings SET isEnrolled = :isEnrolled, enrolledCount = enrolledCount + :delta WHERE id = :trainingId")
    suspend fun updateEnrollment(trainingId: String, isEnrolled: Boolean, delta: Int)

    // Events
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestampMillis DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)

    @Query("DELETE FROM notifications WHERE title IN (:titles)")
    suspend fun deleteNotificationsByTitles(titles: List<String>)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    // Surveys
    @Query("SELECT * FROM surveys")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: SurveyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<SurveyEntity>)

    @Query("DELETE FROM surveys WHERE id = :surveyId")
    suspend fun deleteSurvey(surveyId: String)

    @Query("UPDATE surveys SET isCompleted = 1 WHERE id = :surveyId")
    suspend fun completeSurvey(surveyId: String)

    // Success Stories
    @Query("SELECT * FROM success_stories")
    fun getAllSuccessStories(): Flow<List<SuccessStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuccessStory(story: SuccessStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuccessStories(stories: List<SuccessStoryEntity>)

    @Query("DELETE FROM success_stories WHERE id = :storyId")
    suspend fun deleteSuccessStory(storyId: String)

    @Query("UPDATE success_stories SET isLiked = :isLiked, likesCount = likesCount + :delta WHERE id = :storyId")
    suspend fun updateStoryLike(storyId: String, isLiked: Boolean, delta: Int)

    // Safety Audits
    @Query("SELECT * FROM safety_audits ORDER BY id DESC")
    fun getAllAudits(): Flow<List<SafetyAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: SafetyAuditEntity)

    // IA Sessions & Messages
    @Query("SELECT * FROM hse_ia_sessions ORDER BY timestampMillis DESC")
    fun getAllIaSessions(): Flow<List<HseIaSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIaSession(session: HseIaSessionEntity)

    @Query("DELETE FROM hse_ia_sessions WHERE id = :sessionId")
    suspend fun deleteIaSession(sessionId: String)

    @Query("SELECT * FROM hse_ia_messages WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun getIaMessagesForSession(sessionId: String): Flow<List<HseIaMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIaMessage(message: HseIaMessageEntity): Long

    @Query("DELETE FROM hse_ia_messages WHERE sessionId = :sessionId")
    suspend fun deleteIaMessagesForSession(sessionId: String)
}

