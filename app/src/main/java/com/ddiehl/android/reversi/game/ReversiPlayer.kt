package com.ddiehl.android.reversi.game

class ReversiPlayer(val color: ReversiColor, var name: String?) {
    var score: Int = 0
    var isCPU: Boolean = false
        private set

    init {
        isCPU = false
        score = 0
    }

    fun isCPU(b: Boolean) {
        isCPU = b
    }

    companion object {
        private val TAG = ReversiPlayer::class.java.simpleName
    }
}
