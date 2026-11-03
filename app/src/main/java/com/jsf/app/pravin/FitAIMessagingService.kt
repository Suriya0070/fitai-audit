package com.jsf.app.pravin

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jsf.app.pravin.FitAIApplication.Companion.CHANNEL_COMMUNITY
import com.jsf.app.pravin.FitAIApplication.Companion.CHANNEL_TRAINER_ALERTS
import com.jsf.app.pravin.FitAIApplication.Companion.CHANNEL_WORKOUT_REMINDERS

class FitAIMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO Phase 2: upload token to Firestore users/{uid}.fcmToken so the backend
        // can send targeted push notifications to this device.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val type = message.data["type"] ?: "general"

        val channel = when (type) {
            "trainer_alert" -> CHANNEL_TRAINER_ALERTS
            "community"     -> CHANNEL_COMMUNITY
            else            -> CHANNEL_WORKOUT_REMINDERS
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }

        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .apply { pendingIntent?.let { setContentIntent(it) } }
            .build()

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
