package com.ddiehl.android.reversi

import android.content.SharedPreferences

class SPSettings(val prefs: SharedPreferences) {

    private val PREF_PLAYER_NAME = "pref_player_name"
    private val PREF_AI_DIFFICULTY = "pref_ai_difficulty"

    var playerName: String
        get() = prefs.getString(PREF_PLAYER_NAME, "Foo")
        set(value) {
            prefs.edit().putString(PREF_PLAYER_NAME, value).apply()
        }

    var aiDifficulty: String
        get() = prefs.getString(PREF_AI_DIFFICULTY, "1")
        set(value) {
            prefs.edit().putString(PREF_AI_DIFFICULTY, value).apply()
        }
}
