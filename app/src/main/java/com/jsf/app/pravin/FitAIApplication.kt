package com.jsf.app.pravin

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FitAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase initialises automatically from google-services.json via the Gradle plugin.
        // The manual FirebaseOptions init (and FirebaseConfig.kt) has been removed.
        FirebaseApp.initializeApp(this)

        // Crashlytics: disable in debug to keep logs clean; enable for all release builds.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // App Check: use the Play Integrity provider in release, debug provider in debug builds.
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
    }
}
