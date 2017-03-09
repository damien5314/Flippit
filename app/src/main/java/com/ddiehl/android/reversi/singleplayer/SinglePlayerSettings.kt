package com.ddiehl.android.reversi.singleplayer

import android.content.Context
import android.content.SharedPreferences
import com.ddiehl.android.reversi.R

class SinglePlayerSettings(context: Context) {

    companion object {
        private val PREF_PLAYER_NAME = "PREF_PLAYER_NAME"
        private val PREF_AI_DIFFICULTY = "PREF_AI_DIFFICULTY"
    }

    val context: Context = context.applicationContext

    val prefs: SharedPreferences =
            context.getSharedPreferences("PREFS_SINGLE_PLAYER_SETTINGS", Context.MODE_PRIVATE)

    var playerName: String
        get() = prefs.getString(PREF_PLAYER_NAME, context.getString(R.string.player1_label_default))
        set(value) = prefs.edit().putString(PREF_PLAYER_NAME, value).apply()


    var aiDifficulty: Int
        get() = prefs.getInt(PREF_AI_DIFFICULTY, 1)
        set(value) = prefs.edit().putInt(PREF_AI_DIFFICULTY, value).apply()
}
