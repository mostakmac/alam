package com.mediashare.shared

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

data class FirebaseClientConfig(
    val apiKey: String,
    val projectId: String,
    val storageBucket: String,
    val senderId: String,
    val appId: String,
    val appName: String
) {
    fun isComplete(): Boolean =
        apiKey.isNotBlank() &&
            projectId.isNotBlank() &&
            storageBucket.isNotBlank() &&
            senderId.isNotBlank() &&
            appId.isNotBlank()
}

object FirebaseBootstrap {
    fun initialize(context: Context, config: FirebaseClientConfig): FirebaseApp? {
        if (!config.isComplete()) return null
        FirebaseApp.getApps(context).firstOrNull { it.name == config.appName }?.let { return it }

        val options = FirebaseOptions.Builder()
            .setApiKey(config.apiKey)
            .setProjectId(config.projectId)
            .setStorageBucket(config.storageBucket)
            .setGcmSenderId(config.senderId)
            .setApplicationId(config.appId)
            .build()
        return FirebaseApp.initializeApp(context, options, config.appName)
    }
}
