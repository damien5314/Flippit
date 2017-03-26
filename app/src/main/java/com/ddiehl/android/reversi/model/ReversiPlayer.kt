package com.ddiehl.android.reversi.model

import com.google.android.gms.games.multiplayer.Participant

class ReversiPlayer(
        val color: ReversiColor,
        var name: String = "",
        val gpg: Participant? = null
) {
    var score: Int = 0
    var isCPU: Boolean = false
}
