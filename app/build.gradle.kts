plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    // SOURCE NAMESPACE: unchanged — renaming all source packages is a separate task.
    // The applicationId (Play Store identifier) has been updated to the FitAI brand.
    namespace = "com.jsf.app.pravin"
    compileSdk = 37

    defaultConfig {
        // ACTION REQUIRED: After changing this applicationId you must:
        //   1. Add a new Android app in Firebase Console with package name "com.fitai.app"
        //   2. Download the updated google-services.json and replace app/google-services.json
        //   3. Add your release keystore SHA-1 to the new Firebase app entry
        applicationId = "com.fitai.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // ACTION REQUIRED: Generate your keystore before building a release APK:
        //   keytool -genkey -v -keystore fitai-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias fitai
        // Store the .jks file OUTSIDE the project directory (never commit it to source control).
        // Set KEYSTORE_PASS and KEY_PASS as environment variables in your CI pipeline and local shell.
        create("release") {
            storeFile = file("../fitai-release.jks")
            storePassword = System.getenv("KEYSTORE_PASS")
                ?: error("KEYSTORE_PASS environment variable is not set. Set it before building a release.")
            keyAlias = "fitai"
            keyPassword = System.getenv("KEY_PASS")
                ?: error("KEY_PASS environment variable is not set. Set it before building a release.")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts += "**/*.so"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Camera
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // MediaPipe
    implementation(libs.mediapipe.tasks.vision)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Accompanist
    implementation(libs.accompanist.permissions)
    // Gson
    implementation(libs.gson)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // Coil
    implementation(libs.coil.compose)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    // Coroutines Play Services (for Firebase .await())
    implementation(libs.kotlinx.coroutines.play.services)
    // WebRTC
    implementation(libs.stream.webrtc.android)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
