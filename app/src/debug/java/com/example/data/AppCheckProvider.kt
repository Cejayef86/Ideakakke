package com.example.data

import android.content.Context
import android.content.Intent
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

internal fun installAppCheckProvider(context: Context, intent: Intent?) {
    val token = intent?.getStringExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")
    if (!token.isNullOrBlank()) {
        val key = FirebaseApp.getInstance().persistenceKey
        check(context.getSharedPreferences("com.google.firebase.appcheck.debug.store.$key", Context.MODE_PRIVATE)
            .edit().putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", token).commit())
    }
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
