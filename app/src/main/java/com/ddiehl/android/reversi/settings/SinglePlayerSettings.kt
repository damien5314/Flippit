package com.ddiehl.android.reversi.settings

import android.content.Context
import android.content.SharedPreferences
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.game.AiDifficulty

class SinglePlayerSettings(context: Context) {

    companion object {
        private val PREF_PLAYER_NAME = "PREF_PLAYER_NAME"
        private val PREF_AI_DIFFICULTY = "PREF_AI_DIFFICULTY"
    }

    val context: Context = context.applicationContext

    val prefs: SharedPreferences =
            context.getSharedPreferences("PREFS_SINGLE_PLAYER_SETTINGS", Context.MODE_PRIVATE)

    var playerName: String
        get() = prefs.getString(PREF_PLAYER_NAME, context.getString(R.string.player1_label))
        set(value) = prefs.edit().putString(PREF_PLAYER_NAME, value).apply()

    var aiDifficulty: AiDifficulty
        get() {
            val pref = prefs.getInt(SinglePlayerSettings.PREF_AI_DIFFICULTY, AiDifficulty.EASY.value)
            return AiDifficulty.valueOf(pref)
        }
        set(value) = prefs.edit().putInt(PREF_AI_DIFFICULTY, value.value).apply()
}
