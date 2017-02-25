package com.ddiehl.android.reversi.game


import com.ddiehl.android.reversi.IllegalMoveException
import rx.Observable
import rx.functions.Func0
import java.util.*

class Board(private val rows: Int, private val columns: Int) {

    private val spaces = Array(rows) {
        arrayOfNulls<BoardSpace>(columns)
    }

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

    init {
        reset()
    }

    constructor(rows: Int, cols: Int, `in`: ByteArray) : this(rows, cols) {

        var index = 0
        for (y in 0..height() - 1) {
            for (x in 0..width() - 1) {
                val c = `in`[index++]

                when (c) {
                    0.toByte() -> { }
                    1.toByte() -> getSpaceAt(x, y)!!.color = ReversiColor.Light
                    2.toByte() -> getSpaceAt(x, y)!!.color = ReversiColor.Dark
                }
            }
        }
    }

    constructor(rows: Int, cols: Int, `in`: String) : this(rows, cols) {

        var index = 0
        for (y in 0..height() - 1) {
            for (x in 0..width() - 1) {
                val c = `in`[index++]

                when (c) {
                    '0' -> { }
                    '1' -> getSpaceAt(x, y)!!.color = ReversiColor.Light
                    '2' -> getSpaceAt(x, y)!!.color = ReversiColor.Dark
                }
            }
        }
    }

    fun copy(): Board {
        val copy = Board(rows, columns)
        for (y in 0..rows - 1) {
            for (x in 0..columns - 1) {
                copy.spaces[y][x] = spaces[y][x]!!.copy()
            }
        }
        return copy
    }

    fun reset() {
        for (y in 0..rows - 1) {
            for (x in 0..columns - 1) {
                spaces[y][x] = BoardSpace(x, y)
            }
        }
        spaces[3][3]!!.color = ReversiColor.Light
        spaces[3][4]!!.color = ReversiColor.Dark
        spaces[4][4]!!.color = ReversiColor.Light
        spaces[4][3]!!.color = ReversiColor.Dark
    }

    fun hasMove(c: ReversiColor): Boolean {
        val i = iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (s.isOwned)
                continue
            for (move in MOVE_DIRECTIONS) {
                val value = moveValueInDirection(s, move[0].toInt(), move[1].toInt(), c)
                if (value != 0) {
                    return true
                }
            }
        }
        return false
    }

    fun getSpaceAt(x: Int, y: Int): BoardSpace? =
        if (x >= 0 && x < columns && y >= 0 && y < rows) {
            spaces[y][x]
        } else {
            null
        }

    fun requestClaimSpace(x: Int, y: Int, color: ReversiColor): Observable<Boolean> {
        return Observable.defer(Func0 {
            val space = spaces[x][y]

            // If space is already claimed, return an error
            if (space!!.isOwned) {
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
        for (move in MOVE_DIRECTIONS) {
            if (moveValueInDirection(space, move[0].toInt(), move[1].toInt(), playerColor) != 0) {
                flipInDirection(space, move[0].toInt(), move[1].toInt(), playerColor)
            }
        }
    }

    fun spacesCapturedWithMove(s: BoardSpace, playerColor: ReversiColor): Int {
        var moveVal = 0
        for (move in MOVE_DIRECTIONS)
            moveVal += moveValueInDirection(s, move[0].toInt(), move[1].toInt(), playerColor)
        return moveVal
    }

    private fun moveValueInDirection(s: BoardSpace, dx: Int, dy: Int, playerColor: ReversiColor): Int {
        if (s.x() + dx < 0 || s.x() + dx >= columns || s.y() + dy < 0 || s.y() + dy >= rows)
            return 0

        var moveVal = 0
        val opponentColor = if (playerColor == ReversiColor.Dark) ReversiColor.Light else ReversiColor.Dark
        val firstPiece = getSpaceAt(s.x() + dx, s.y() + dy)

        if (firstPiece != null && firstPiece.color == opponentColor) {
            var cx = s.x() + dx
            var cy = s.y() + dy
            while (getSpaceAt(cx, cy) != null && getSpaceAt(cx, cy)!!.color == opponentColor) {
                moveVal++
                cx += dx
                cy += dy
            }
            if (getSpaceAt(cx, cy) != null && getSpaceAt(cx, cy)!!.color == playerColor) {
                return moveVal
            }
        }

        return 0
    }

    private fun flipInDirection(s: BoardSpace, dx: Int, dy: Int, playerColor: ReversiColor) {
        s.color = playerColor
        var cx = s.x() + dx
        var cy = s.y() + dy

        while (getSpaceAt(cx, cy)!!.color != playerColor) {
            getSpaceAt(cx, cy)!!.flipColor()
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

    fun width(): Int {
        return columns
    }

    fun height(): Int {
        return rows
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
        return spaces[row][col]!!
    }

    class BoardIterator(private val mBoard: Board) : Iterator<BoardSpace> {
        private var x = 0
        private var y = 0

        override fun hasNext(): Boolean {
            return y != mBoard.height()
        }

        override fun next(): BoardSpace {
            if (!hasNext()) throw NoSuchElementException()

            val s = mBoard.getSpaceAt(x, y)
            if (++x == mBoard.width()) {
                y++
                x = 0
            }

            return s!!
        }
    }


    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("\n")
        for (y in 0..rows - 1) {
            for (x in 0..columns - 1) {
                val s = getSpaceAt(x, y)
                if (!s!!.isOwned)
                    sb.append("0 ")
                else if (s.color == ReversiColor.Light)
                    sb.append("1 ")
                else
                    sb.append("2 ")
            }
            if (y != spaces.size - 1)
                sb.append("\n")
        }
        return sb.toString()
    }
}
