package com.ddiehl.android.reversi.model

data class BoardSpace(private val row: Int, private val col: Int, var color: ReversiColor? = null) {

    val x: Int = col
    val y: Int = row

    val isOwned: Boolean = color != null

    fun flip(): ReversiColor? {
        val c = color
        if (c == null) {
            return null
        } else {
            color = c.opposite()
            return color
        }
    }
}
