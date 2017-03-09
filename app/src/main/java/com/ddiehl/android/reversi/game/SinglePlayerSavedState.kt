package com.ddiehl.android.reversi.game

import android.content.Context
import android.content.SharedPreferences
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.model.ReversiPlayer

class SinglePlayerSavedState(context: Context) {

    companion object {
        private val PREF_CURRENT_PLAYER = "PREF_CURRENT_PLAYER"
        private val PREF_FIRST_TURN = "PREF_FIRST_TURN"
        private val PREF_BOARD_STATE = "PREF_BOARD_STATE"
    }

    val prefs: SharedPreferences =
            context.getSharedPreferences("PREFS_SINGLE_PLAYER_STATE", Context.MODE_PRIVATE)

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
