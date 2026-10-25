plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun firebaseConfig(name: String): String {
    val value = providers.environmentVariable(name).orElse("").get()
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

android {
    namespace = "com.mediashare.rev"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mediashare.rev"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "FIREBASE_API_KEY", firebaseConfig("FIREBASE_API_KEY"))
        buildConfigField("String", "FIREBASE_PROJECT_ID", firebaseConfig("FIREBASE_PROJECT_ID"))
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", firebaseConfig("FIREBASE_STORAGE_BUCKET"))
        buildConfigField("String", "FIREBASE_SENDER_ID", firebaseConfig("FIREBASE_SENDER_ID"))
        buildConfigField("String", "FIREBASE_APP_ID", firebaseConfig("FIREBASE_APP_ID_REV"))
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("androidx.core:core-ktx:1.15.0")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
