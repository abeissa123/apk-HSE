package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["role"]),
        Index(value = ["status"])
    ]
)
data class UserEntity(
    @PrimaryKey val email: String = "",
    val name: String = "Utilisateur",
    val role: String = "Collaborateur", // "Admin", "Manager", "Collaborateur"
    val site: String = "Site Figuil",
    val phone: String = "",
    val avatarInitials: String = "IA",
    val pushNotificationsEnabled: Boolean = true,
    val mfaEnabled: Boolean = true,
    val department: String = "Sécurité Industrielle",
    val status: String = "Actif", // "Actif", "Inactif", "En attente"
    val isOnline: Boolean = false,
    val lastActiveMillis: Long = System.currentTimeMillis(),
    val password: String = "",
    val firebaseUid: String = "",
    val authSynced: Boolean = true,
    val resetRequested: Boolean = false,
    val resetRequestNote: String = ""
) {
    val isAdmin: Boolean
        get() = role.equals("Admin", ignoreCase = true) ||
                role.equals("Administrateur", ignoreCase = true) ||
                email.equals("abeissajean66@gmail.com", ignoreCase = true) ||
                email.equals("admin.hse@cimencam.cm", ignoreCase = true)


    val isProtectedAdmin: Boolean
        get() = email.equals("abeissajean66@gmail.com", ignoreCase = true) ||
                email.equals("admin.hse@cimencam.cm", ignoreCase = true) ||
                email.equals("admin@holcim.cm", ignoreCase = true) ||
                email.equals("admin@cimencam.cm", ignoreCase = true) ||
                (isAdmin && (email.contains("admin", ignoreCase = true) || role.contains("Principal", ignoreCase = true) || role.contains("Direct", ignoreCase = true) || role.contains("Super", ignoreCase = true)))
}

@Entity(
    tableName = "contents",
    indices = [
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["timestampMillis"])
    ]
)
data class ContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String, // "PDF", "PHOTO", "VIDEO", "LIEN", "TEMOIGNAGE"
    val category: String, // "Sécurité", "Santé", "Environnement", "Induction", "Qualité"
    val fileUrl: String = "",
    val viewsCount: Int = 0,
    val dateText: String = "Aujourd'hui",
    val referenceCode: String = "",
    val version: String = "v1.0",
    val authorName: String = "Direction",
    val authorRole: String = "Administrateur",
    val site: String = "Tous sites",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_attestations",
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["userEmail"]),
        Index(value = ["timestampMillis"])
    ]
)
data class DocumentAttestationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: String,
    val documentTitle: String,
    val documentReference: String,
    val userEmail: String,
    val userName: String,
    val userRole: String,
    val userSite: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val timestampText: String,
    val isCertified: Boolean = true,
    val certificateId: String = ""
)

@Entity(
    tableName = "incidents",
    indices = [
        Index(value = ["status"]),
        Index(value = ["category"]),
        Index(value = ["site"]),
        Index(value = ["timestampMillis"])
    ]
)
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Sécurité", "Santé", "Environnement", "Presqu'accident"
    val severity: String, // "Critique", "Majeur", "Mineur"
    val site: String, // "Site Figuil", "Site Bonabéri", "Site Nomayos"
    val location: String, // e.g. "Atelier de maintenance"
    val description: String,
    val reportedBy: String,
    val timestampText: String, // e.g. "Il y a 2h"
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "En cours" // "En cours", "Traité", "Validé"
)

@Entity(tableName = "procedure_steps")
data class ProcedureStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val procedureId: String,
    val stepNumber: Int,
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "trainings")
data class TrainingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val instructor: String,
    val site: String,
    val durationHours: Int,
    val dayNumber: String,
    val monthShort: String,
    val year: String = "2026",
    val enrolledCount: Int,
    val maxCapacity: Int = 15,
    val status: String = "Planifié",
    val isEnrolled: Boolean = false
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val tag: String,
    val status: String,
    val dateText: String,
    val description: String,
    val site: String = "Tous les sites"
)

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["isRead"]),
        Index(value = ["timestampMillis"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "Urgence", "Sécurité", "Formation", "Information"
    val timestampText: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val status: String = "Actif",
    val quarter: String = "T3 2026",
    val questionsCount: Int = 5,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "success_stories",
    indices = [
        Index(value = ["type"])
    ]
)
data class SuccessStoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val authorName: String,
    val authorRole: String,
    val site: String,
    val dateText: String,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val type: String, // "PHOTO", "TEMOIGNAGE", "VIDEO", "BONNE_PRATIQUE", "INNOVATION"
    val mediaUrl: String = "",
    val durationText: String = "",
    val resolution: String = ""
)

@Entity(tableName = "safety_audits")
data class SafetyAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,
    val auditorName: String,
    val dateText: String,
    val scorePercentage: Int,
    val status: String = "Conforme"
)

@Entity(tableName = "hse_ia_sessions")
data class HseIaSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String = "CHAT", // "CHAT", "ANALYSE_RISQUE", "PHOTO_ANALYSE", "CHECKLIST", "PLAN_ACTION", "RAPPORT_INCIDENT", "EPI_CONSEIL"
    val timestampMillis: Long = System.currentTimeMillis(),
    val lastMessageText: String = "",
    val site: String = "Tous sites"
)

@Entity(
    tableName = "hse_ia_messages",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestampMillis"])
    ]
)
data class HseIaMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val sender: String, // "USER", "IA"
    val text: String,
    val imageUri: String? = null,
    val category: String = "CHAT",
    val timestampMillis: Long = System.currentTimeMillis(),
    val isUrgentWarning: Boolean = false
)

