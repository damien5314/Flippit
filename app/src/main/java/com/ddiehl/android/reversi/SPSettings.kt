package com.ddiehl.android.reversi

import android.content.Context
import android.content.SharedPreferences

class SPSettings(context: Context) {

    companion object {
        private val PREF_PLAYER_NAME = "PREF_PLAYER_NAME"
        private val PREF_AI_DIFFICULTY = "PREF_AI_DIFFICULTY"
    }

    val context: Context = context.applicationContext

    val prefs: SharedPreferences =
            context.getSharedPreferences("PREFS_SINGLE_PLAYER_SETTINGS", Context.MODE_PRIVATE)

    var playerName: String
        get() = prefs.getString(PREF_PLAYER_NAME, context.getString(R.string.player1_label_default))
        set(value) {
            prefs.edit().putString(PREF_PLAYER_NAME, value).apply()
        }

    var aiDifficulty: String
        get() = prefs.getString(PREF_AI_DIFFICULTY, "1")
        set(value) {
            prefs.edit().putString(PREF_AI_DIFFICULTY, value).apply()
        }
}
