package com.example.service

import android.content.Context
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmPushManager {

    private const val TAG = "FcmPushManager"

    // Core Global & Field Topics
    const val TOPIC_ALL_USERS = "hse_all_users"
    const val TOPIC_SAFETY_ALERTS = "hse_safety_alerts"
    const val TOPIC_UPDATES = "hse_updates"
    const val TOPIC_FIELD_WORKERS = "hse_field_workers"
    const val TOPIC_SUCCESS_STORIES = "hse_success_stories"
    const val TOPIC_IMPORTANT_SURVEYS = "hse_important_surveys"

    @Volatile
    private var isFcmAvailable: Boolean = false

    fun getSiteTopic(site: String): String {
        val clean = site.trim().lowercase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("à", "a")
            .replace(" ", "_")
            .replace("-", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
        return "hse_site_$clean"
    }

    private fun isPlayServicesAvailable(context: Context): Boolean {
        return try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            resultCode == ConnectionResult.SUCCESS
        } catch (e: Throwable) {
            false
        }
    }

    fun initialize(context: Context, userEmail: String? = null, userSite: String? = null) {
        if (!userEmail.isNullOrBlank()) {
            val prefs = context.getSharedPreferences("hse_fcm_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("user_email", userEmail).apply()
        }

        if (!isPlayServicesAvailable(context)) {
            Log.d(TAG, "Google Play Services non disponible sur ce terminal. Mode local sécurisé.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fcm = FirebaseMessaging.getInstance()
                val token = try {
                    fcm.token.await()
                } catch (e: Throwable) {
                    Log.d(TAG, "FCM registration token non disponible actuellement : ${e.message}")
                    null
                }

                if (!token.isNullOrBlank()) {
                    isFcmAvailable = true
                    Log.d(TAG, "FCM Registration Token initialisé avec succès : $token")
                    if (!userEmail.isNullOrBlank()) {
                        updateTokenInFirestore(userEmail, token, userSite)
                    }
                    subscribeCoreTopics(fcm, userSite)
                }
            } catch (e: Throwable) {
                Log.d(TAG, "FCM initialisation terminée en mode dégradé sécurisé : ${e.message}")
            }
        }
    }

    private fun subscribeCoreTopics(fcm: FirebaseMessaging, userSite: String? = null) {
        if (!isFcmAvailable) return
        try {
            fcm.subscribeToTopic(TOPIC_ALL_USERS)
            fcm.subscribeToTopic(TOPIC_SAFETY_ALERTS)
            fcm.subscribeToTopic(TOPIC_UPDATES)
            fcm.subscribeToTopic(TOPIC_FIELD_WORKERS)
            fcm.subscribeToTopic(TOPIC_SUCCESS_STORIES)
            fcm.subscribeToTopic(TOPIC_IMPORTANT_SURVEYS)

            if (!userSite.isNullOrBlank() && userSite != "Tous les sites") {
                val siteTopic = getSiteTopic(userSite)
                fcm.subscribeToTopic(siteTopic)
                Log.d(TAG, "Subscribed worker to site topic: $siteTopic")
            }
        } catch (te: Throwable) {
            Log.d(TAG, "Topic subscription optionnelle ignorée : ${te.message}")
        }
    }

    fun subscribeUser(context: Context, email: String?, site: String? = null, role: String? = null) {
        if (email.isNullOrBlank()) return
        if (!isPlayServicesAvailable(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fcm = FirebaseMessaging.getInstance()
                val token = try {
                    fcm.token.await()
                } catch (e: Throwable) {
                    null
                }

                if (!token.isNullOrBlank()) {
                    isFcmAvailable = true
                    subscribeCoreTopics(fcm, site)
                    updateTokenInFirestore(email, token, site, role)
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Souscription utilisateur FCM ignorée : ${e.message}")
            }
        }
    }

    fun updateTokenInFirestore(email: String, token: String, site: String? = null, role: String? = null) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val data = mutableMapOf<String, Any>(
                "fcmToken" to token,
                "lastTokenUpdate" to System.currentTimeMillis()
            )
            if (!site.isNullOrBlank()) data["site"] = site
            if (!role.isNullOrBlank()) data["role"] = role

            firestore.collection("users").document(email)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to Firestore for $email")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save FCM token: ${e.message}")
        }
    }

    // High-priority Emergency & Safety Alert Broadcast
    suspend fun broadcastSafetyAlert(
        title: String,
        message: String,
        site: String = "Tous les sites",
        priority: String = "URGENT"
    ) {
        val topic = if (site.isBlank() || site == "Tous les sites") {
            TOPIC_SAFETY_ALERTS
        } else {
            getSiteTopic(site)
        }
        broadcastPushNotification(
            title = title,
            message = message,
            topic = topic,
            type = "ALERTE",
            site = site,
            priority = priority
        )
    }

    // Safety Updates, Procedures, Directives Broadcast
    suspend fun broadcastUpdateNotification(
        title: String,
        message: String,
        type: String = "INFO",
        site: String = "Tous les sites",
        priority: String = "NORMAL"
    ) {
        val topic = if (site.isBlank() || site == "Tous les sites") {
            TOPIC_UPDATES
        } else {
            getSiteTopic(site)
        }
        broadcastPushNotification(
            title = title,
            message = message,
            topic = topic,
            type = type,
            site = site,
            priority = priority
        )
    }

    suspend fun broadcastPushNotification(
        title: String,
        message: String,
        topic: String,
        type: String = "ALERT",
        site: String = "Tous les sites",
        priority: String = "NORMAL"
    ) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val broadcastId = "push_${System.currentTimeMillis()}"
            val payload = mapOf(
                "id" to broadcastId,
                "title" to title,
                "message" to message,
                "topic" to topic,
                "type" to type,
                "site" to site,
                "priority" to priority,
                "timestampMillis" to System.currentTimeMillis(),
                "status" to "BROADCASTED"
            )
            // Log push broadcast in Firestore for instant multi-device distribution & backend triggers
            firestore.collection("push_broadcasts").document(broadcastId)
                .set(payload)
                .await()

            // If it's a safety alert, also log in dedicated safety_alerts collection
            if (type.equals("ALERTE", ignoreCase = true) || type.equals("URGENCE", ignoreCase = true) || priority.equals("URGENT", ignoreCase = true)) {
                firestore.collection("safety_alerts").document(broadcastId)
                    .set(payload)
                    .await()
            }
            Log.d(TAG, "Push broadcast created successfully for topic: $topic ($title)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to queue push notification: ${e.message}")
        }
    }
}

