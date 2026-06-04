package com.elara.app.ui.main

import android.content.Context

object VoiceSettings {

    private const val PREFS = "elara_voice"

    fun savePitch(context: Context, pitch: Float) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("pitch", pitch).apply()

    fun saveRate(context: Context, rate: Float) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("rate", rate).apply()

    fun getPitch(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("pitch", 1.1f)

    fun getRate(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("rate", 0.95f)

    fun setFemale(context: Context) { savePitch(context, 1.3f); saveRate(context, 0.95f) }
    fun setMale(context: Context) { savePitch(context, 0.7f); saveRate(context, 0.9f) }
}
