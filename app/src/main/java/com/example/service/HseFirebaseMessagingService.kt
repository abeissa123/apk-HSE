package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.entity.NotificationEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HseFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HseFcmService"
        
        // Safety alerts channel (Critical, High priority, Loud alarm/vibration)
        const val CHANNEL_ID_SAFETY_ALERTS = "hse_safety_alerts_channel"
        const val CHANNEL_NAME_SAFETY_ALERTS = "Alertes d'Urgence & Sécurité Terrain"
        
        // General updates channel (Directives, Stories, Surveys, Info)
        const val CHANNEL_ID_UPDATES = "hse_updates_channel"
        const val CHANNEL_NAME_UPDATES = "Mises à jour & Directives HSE"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        val prefs = getSharedPreferences("hse_fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        val email = prefs.getString("user_email", null)
        if (!email.isNullOrBlank()) {
            FcmPushManager.updateTokenInFirestore(email, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Alerte Sécurité HSE"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "Nouvelle consigne de sécurité terrain disponible."

        val type = remoteMessage.data["type"] ?: "ALERTE"
        val site = remoteMessage.data["site"] ?: "Tous les sites"
        val priority = remoteMessage.data["priority"] ?: "URGENT"

        // 1. Save notification immediately to Room Database so it appears in in-app Notifications center
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val notif = NotificationEntity(
                    title = title,
                    message = body,
                    type = type,
                    timestampText = "À l'instant",
                    timestampMillis = System.currentTimeMillis(),
                    isRead = false
                )
                db.hseDao().insertNotification(notif)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save incoming FCM message to Room DB: ${e.message}")
            }
        }

        // 2. Display the system notification to the field worker
        showNotification(title, body, type, site, priority)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        site: String,
        priority: String
    ) {
        val isEmergency = type.equals("URGENCE", ignoreCase = true) ||
                type.equals("ALERTE", ignoreCase = true) ||
                priority.equals("URGENT", ignoreCase = true) ||
                priority.equals("CRITICAL", ignoreCase = true)

        val channelId = if (isEmergency) CHANNEL_ID_SAFETY_ALERTS else CHANNEL_ID_UPDATES

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("notification_type", type)
            putExtra("notification_site", site)
            putExtra("notification_priority", priority)
            if (isEmergency) {
                putExtra("target_tab", "URGENCE")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 100000).toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = if (isEmergency) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channels for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Safety Alerts Channel
            val safetyChannel = NotificationChannel(
                CHANNEL_ID_SAFETY_ALERTS,
                CHANNEL_NAME_SAFETY_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes critiques, évacuations et consignes de sécurité immédiates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(safetyChannel)

            // Updates Channel
            val updatesChannel = NotificationChannel(
                CHANNEL_ID_UPDATES,
                CHANNEL_NAME_UPDATES,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mises à jour des procédures, flashs d'information, sondages et succès"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            notificationManager.createNotificationChannel(updatesChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setSummaryText(if (isEmergency) "⚠️ ALERTE TERRAIN" else "HSE Cimencam"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(if (isEmergency) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isEmergency) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        if (isEmergency) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
        }

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notificationBuilder.build())
    }
}
