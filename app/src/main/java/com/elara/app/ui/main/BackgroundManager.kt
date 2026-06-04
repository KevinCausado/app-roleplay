package com.elara.app.ui.main

import android.content.Context
import android.net.Uri

object BackgroundManager {

    private const val PREFS = "elara_backgrounds"

    fun save(context: Context, worldName: String, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(worldName, uri.toString()).apply()
    }

    fun get(context: Context, worldName: String): Uri? {
        val str = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(worldName, null) ?: return null
        return Uri.parse(str)
    }
}
