package com.ddiehl.android.reversi.model

enum class ReversiColor {
    LIGHT, DARK;

    fun opposite(): ReversiColor {
        if (this == LIGHT) {
            return DARK
        } else {
            return LIGHT
        }
    }
}
