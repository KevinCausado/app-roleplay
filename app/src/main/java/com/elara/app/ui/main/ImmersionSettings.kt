package com.elara.app.ui.main

import android.content.Context

object ImmersionSettings {
    private const val PREFS = "elara_immersion"

    fun setNoTranslation(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("no_translation", enabled).apply()

    fun isNoTranslation(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("no_translation", false)
}
