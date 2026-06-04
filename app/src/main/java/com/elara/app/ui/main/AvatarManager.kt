package com.elara.app.ui.main

import android.content.Context
import android.net.Uri

object AvatarManager {

    private const val PREFS = "elara_avatars"

    fun save(context: Context, characterId: String, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(characterId, uri.toString()).apply()
    }

    fun get(context: Context, characterId: String): Uri? {
        val str = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(characterId, null) ?: return null
        return Uri.parse(str)
    }
}
