package com.ddiehl.android.reversi.view

import android.content.SharedPreferences
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.model.ReversiPlayer

class SPSavedState(val prefs: SharedPreferences) {

    private val PREF_CURRENT_PLAYER = "pref_currentPlayer"
    private val PREF_FIRST_TURN = "pref_firstTurn"
    private val PREF_BOARD_STATE = "pref_boardState"

    var currentPlayer: Boolean = true
        get() = prefs.getBoolean(PREF_CURRENT_PLAYER, true)

    var firstTurn: Boolean = true
        get() = prefs.getBoolean(PREF_FIRST_TURN, true)

    var board: String? = null
        get() = prefs.getString(PREF_BOARD_STATE, null)

    fun save(board: Board, currentPlayer: ReversiPlayer, playerWithFirstTurn: ReversiPlayer) {
        val bytes = board.serialize()
        val out = StringBuilder()
        for (b in bytes) {
            out.append(b.toInt())
        }

        prefs.edit()
                .putBoolean(PREF_CURRENT_PLAYER, currentPlayer.color == ReversiColor.LIGHT)
                .putBoolean(PREF_FIRST_TURN, playerWithFirstTurn.color == ReversiColor.LIGHT)
                .putString(PREF_BOARD_STATE, out.toString())
                .apply()
    }

    fun clear() {
        prefs.edit()
                .remove(PREF_CURRENT_PLAYER)
                .remove(PREF_FIRST_TURN)
                .remove(PREF_BOARD_STATE)
                .apply()
    }
}
