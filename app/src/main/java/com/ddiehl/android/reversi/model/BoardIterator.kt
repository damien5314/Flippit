package com.ddiehl.android.reversi.model

import java.util.*

class BoardIterator(private val board: Board) : Iterator<BoardSpace> {
    private var x = 0
    private var y = 0

    override fun hasNext(): Boolean {
        return y != board.height
    }

    override fun next(): BoardSpace {
        if (!hasNext()) throw NoSuchElementException()

        val s = board.getSpaceAt(x, y)
        if (++x == board.width) {
            y++
            x = 0
        }

        return s
    }
}
