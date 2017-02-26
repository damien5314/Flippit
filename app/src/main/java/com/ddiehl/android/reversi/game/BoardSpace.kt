package com.ddiehl.android.reversi.game

class BoardSpace(private val row: Int, private val col: Int) {

    var color: ReversiColor? = null

    fun x(): Int {
        return col
    }

    fun y(): Int {
        return row
    }

    fun copy(): BoardSpace {
        val copy = BoardSpace(row, col)
        copy.color = color
        return copy
    }

    val isOwned: Boolean
        get() = color != null

    fun flipColor(): ReversiColor? {
        if (color == null) {
            return null
        } else {
            color = if (color == ReversiColor.Dark) ReversiColor.Light else ReversiColor.Dark
            return color
        }
    }
}
