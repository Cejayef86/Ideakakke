package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp

private const val TAG = "AppCheckInit"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        installAppCheckProvider(context, intent)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
