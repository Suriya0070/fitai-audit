package com.jsf.app.pravin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings

class FitAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }

        // Enable Firestore offline persistence so community content is readable without connectivity.
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()

        // Analytics auto-collects on non-debug builds; initialising here ensures the instance
        // is warm before the first screen reports.
        FirebaseAnalytics.getInstance(this)
            .setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WORKOUT_REMINDERS,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily reminders to stick to your workout schedule" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRAINER_ALERTS,
                "Trainer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Messages and alerts from your personal trainer" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMMUNITY,
                "Community",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Activity in your fitness community servers" }
        )
    }

    companion object {
        const val CHANNEL_WORKOUT_REMINDERS = "workout_reminders"
        const val CHANNEL_TRAINER_ALERTS = "trainer_alerts"
        const val CHANNEL_COMMUNITY = "community"
    }
}
