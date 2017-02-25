package com.ddiehl.android.reversi.game


import com.ddiehl.android.reversi.IllegalMoveException
import rx.Observable
import rx.functions.Func0
import java.util.*

class Board(val height: Int, val width: Int) {

    companion object {
        private val MOVE_DIRECTIONS = arrayOf(
                byteArrayOf(0, -1), // Down
                byteArrayOf(1, 0), // Right
                byteArrayOf(-1, 0), // Left
                byteArrayOf(0, 1), // Up
                byteArrayOf(-1, -1), // Down-Left
                byteArrayOf(1, -1), // Down-Right
                byteArrayOf(-1, 1), // Top-Left
                byteArrayOf(1, 1) // Top-Right
        )
    }

    private val spaces: Array<Array<BoardSpace>> =
            Array(height) { row ->
                Array(width) { col ->
                    BoardSpace(row, col)
                }
            }

    fun reset() {
        spaces[3][3].color = ReversiColor.Light
        spaces[3][4].color = ReversiColor.Dark
        spaces[4][4].color = ReversiColor.Light
        spaces[4][3].color = ReversiColor.Dark
    }

    constructor(rows: Int, cols: Int, saved: ByteArray) : this(rows, cols) {

        var index = 0
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val c = saved[index++]

                when (c) {
                    0.toByte() -> { }
                    1.toByte() -> getSpaceAt(x, y).color = ReversiColor.Light
                    2.toByte() -> getSpaceAt(x, y).color = ReversiColor.Dark
                }
            }
        }
    }

    constructor(rows: Int, cols: Int, saved: String) : this(rows, cols) {

        var index = 0
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val c = saved[index++]

                when (c) {
                    '0' -> { }
                    '1' -> getSpaceAt(x, y).color = ReversiColor.Light
                    '2' -> getSpaceAt(x, y).color = ReversiColor.Dark
                }
            }
        }
    }

    fun copy(): Board {
        val copy = Board(height, width)
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                copy.spaces[y][x] = spaces[y][x].copy()
            }
        }
        return copy
    }

    fun hasMove(c: ReversiColor): Boolean {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val space = iterator.next()
            if (!space.isOwned) {
                MOVE_DIRECTIONS
                        .map { moveValueInDirection(space, it[0].toInt(), it[1].toInt(), c) }
                        .filter { value -> value != 0 }
                        .first { return true }
            }
        }
        return false
    }

    fun getSpaceAt(x: Int, y: Int): BoardSpace =
            if (x >= 0 && x < width && y >= 0 && y < height) {
                spaces[y][x]
            } else {
                throw NullPointerException("Space @ $x $y is null")
            }

    fun requestClaimSpace(row: Int, col: Int, color: ReversiColor): Observable<Boolean> {
        return Observable.defer(Func0 {
            val space = spaces[row][col]

            // If space is already claimed, return an error
            if (space.isOwned) {
                return@Func0 Observable.error<Boolean>(IllegalMoveException("space is already owned"))
            }

            val captured = spacesCapturedWithMove(space, color)
            if (captured <= 0) {
                return@Func0 Observable.error<Boolean>(IllegalMoveException("move value is <= 0: " + captured))
            }

            commitPiece(space, color)
            Observable.just(true)
        })
    }

    fun commitPiece(space: BoardSpace, playerColor: ReversiColor) {
        MOVE_DIRECTIONS
                .filter { moveValueInDirection(space, it[0].toInt(), it[1].toInt(), playerColor) != 0 }
                .forEach { flipInDirection(space, it[0].toInt(), it[1].toInt(), playerColor) }
    }

    fun spacesCapturedWithMove(s: BoardSpace, playerColor: ReversiColor): Int =
        MOVE_DIRECTIONS.sumBy {
            moveValueInDirection(s, it[0].toInt(), it[1].toInt(), playerColor)
        }

    private fun moveValueInDirection(s: BoardSpace, dx: Int, dy: Int, playerColor: ReversiColor): Int {
        if (s.x() + dx < 0 || s.x() + dx >= width || s.y() + dy < 0 || s.y() + dy >= height)
            return 0

        var moveVal = 0
        val opponentColor = if (playerColor == ReversiColor.Dark) ReversiColor.Light else ReversiColor.Dark
        val firstPiece = getSpaceAt(s.x() + dx, s.y() + dy)

        if (firstPiece.color == opponentColor) {
            var cx = s.x() + dx
            var cy = s.y() + dy
            while (getSpaceAt(cx, cy).color == opponentColor) {
                moveVal++
                cx += dx
                cy += dy
            }
            if (getSpaceAt(cx, cy).color == playerColor) {
                return moveVal
            }
        }

        return 0
    }

    private fun flipInDirection(space: BoardSpace, dx: Int, dy: Int, playerColor: ReversiColor) {
        space.color = playerColor
        var cx = space.x() + dx
        var cy = space.y() + dy

        while (getSpaceAt(cx, cy).color != playerColor) {
            getSpaceAt(cx, cy).flipColor()
            cx += dx
            cy += dy
        }
    }

    fun getNumSpacesForColor(c: ReversiColor): Int {
        var count = 0
        val i = iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (s.isOwned && s.color == c)
                count++
        }
        return count
    }

    val numberOfEmptySpaces: Int
        get() {
            var count = 0
            val i = iterator()
            while (i.hasNext()) {
                val s = i.next()
                if (!s.isOwned)
                    count++
            }
            return count
        }

    fun serialize(): ByteArray {
        val out = ByteArray(64)
        var index = 0
        val i = iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (!s.isOwned)
                out[index++] = 0
            else if (s.color == ReversiColor.Light)
                out[index++] = 1
            else
                out[index++] = 2
        }
        return out
    }

    fun getSpaceNumber(s: BoardSpace): Byte {
        return (s.y() * 8 + s.x() + 1).toByte()
    }

    fun getBoardSpaceFromNum(n: Int): BoardSpace? {
        var n2 = n
        n2 -= 1
        return getSpaceAt(n % 8, n / 8)
    }

    fun iterator(): BoardIterator {
        return BoardIterator(this)
    }

    fun spaceAt(row: Int, col: Int): BoardSpace {
        return spaces[row][col]
    }

    class BoardIterator(private val mBoard: Board) : Iterator<BoardSpace> {
        private var x = 0
        private var y = 0

        override fun hasNext(): Boolean {
            return y != mBoard.height
        }

        override fun next(): BoardSpace {
            if (!hasNext()) throw NoSuchElementException()

            val s = mBoard.getSpaceAt(x, y)
            if (++x == mBoard.width) {
                y++
                x = 0
            }

            return s
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("\n")
        for (row in 0..height - 1) {
            (0..width - 1)
                    .map { col -> getSpaceAt(col, row) }
                    .forEach {
                        if (!it.isOwned)
                            sb.append("0 ")
                        else if (it.color == ReversiColor.Light)
                            sb.append("1 ")
                        else
                            sb.append("2 ")
                    }
            if (row != spaces.size - 1)
                sb.append("\n")
        }
        return sb.toString()
    }
}
