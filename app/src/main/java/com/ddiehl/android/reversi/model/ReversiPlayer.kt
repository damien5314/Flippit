package com.ddiehl.android.reversi.model

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
}
