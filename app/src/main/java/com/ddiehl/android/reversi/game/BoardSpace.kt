package com.ddiehl.android.reversi.game

class BoardSpace {

    var color: ReversiColor? = null
    private val x: Int
    private val y: Int

    constructor(x: Int, y: Int) {
        color = null
        this.x = x
        this.y = y
    }

    fun x(): Int {
        return x
    }

    fun y(): Int {
        return y
    }

    fun copy(): BoardSpace {
        val copy = BoardSpace(x, y)
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
