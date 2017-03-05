package com.ddiehl.android.reversi.model


import com.ddiehl.android.reversi.IllegalMoveException
import com.ddiehl.android.reversi.utils.Utils
import rx.Observable
import rx.functions.Func0

class Board(val height: Int, val width: Int) {

    data class MoveDirection(val dx: Int, val dy: Int)

    companion object {
        private val MOVE_DIRECTIONS = listOf(
                MoveDirection(0, -1), // Down
                MoveDirection(1, 0), // Right
                MoveDirection(-1, 0), // Left
                MoveDirection(0, 1), // Up
                MoveDirection(-1, -1), // Down-Left
                MoveDirection(1, -1), // Down-Right
                MoveDirection(-1, 1), // Top-Left
                MoveDirection(1, 1) // Top-Right
        )

        fun getBoard(rows: Int, cols: Int, saved: ByteArray): Board {
            return getBoard(rows, cols, Utils.byteArrayToString(saved))
        }

        fun getBoard(rows: Int, cols: Int, saved: String): Board {
            val board = Board(rows, cols)

            var index = 0
            for (y in 0..board.height - 1) {
                for (x in 0..board.width - 1) {
                    val c = saved[index++]

                    when (c) {
                        '0' -> { }
                        '1' -> board.getSpaceAt(x, y).color = ReversiColor.LIGHT
                        '2' -> board.getSpaceAt(x, y).color = ReversiColor.DARK
                    }
                }
            }

            return board
        }
    }

    private val spaces: Array<Array<BoardSpace>> =
            Array(height) { row ->
                Array(width) { col ->
                    BoardSpace(row, col)
                }
            }

    fun reset() {
        // Set the color of each space to null first
        spaces.forEach { row ->
            row.forEach { space ->
                space.color = null
            }
        }

        // Then set the center 4 spaces to the starting configuration
        spaces[3][3].color = ReversiColor.LIGHT
        spaces[3][4].color = ReversiColor.DARK
        spaces[4][4].color = ReversiColor.LIGHT
        spaces[4][3].color = ReversiColor.DARK
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

    fun hasMove(color: ReversiColor): Boolean {
        for (space in iterator()) {
            MOVE_DIRECTIONS
                    .filter { !space.isOwned }
                    .filter { (dx, dy) -> isWithinBounds(space.x + dx, space.y + dy) }
                    .map { (dx, dy) -> moveValueInDirection(space, dx, dy, color) }
                    .filter { value -> value > 0 }
                    .forEach { return true }
        }
        return false
    }

    fun isWithinBounds(x: Int, y: Int): Boolean {
        return x in 0..(width - 1) && y in 0..(height - 1)
    }

    fun getSpaceAt(x: Int, y: Int): BoardSpace =
            if (isWithinBounds(x, y)) {
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
                .filter { (dx, dy) -> moveValueInDirection(space, dx, dy, playerColor) != 0 }
                .forEach { (dx, dy) -> flipInDirection(space, dx, dy, playerColor) }
    }

    internal fun spacesCapturedWithMove(space: BoardSpace, playerColor: ReversiColor): Int =
            MOVE_DIRECTIONS
                    .filter { (dx, dy) -> isWithinBounds(space.x + dx, space.y + dy) }
                    .sumBy { (dx, dy) ->
                        moveValueInDirection(space, dx, dy, playerColor)
                    }

    private fun moveValueInDirection(space: BoardSpace, dx: Int, dy: Int, playerColor: ReversiColor): Int {
        // If the move would bring us out of bounds of the board area, just return 0
        if (!isWithinBounds(space.x + dx, space.y + dy)) {
            return 0
        }

        // Otherwise, calculate how many spaces we can capture in that direction
        var moveVal = 0
        val opponentColor = if (playerColor == ReversiColor.DARK) ReversiColor.LIGHT else ReversiColor.DARK
        val firstPiece = getSpaceAt(space.x + dx, space.y + dy)

        if (firstPiece.color == opponentColor) {
            var currentX = space.x + dx
            var currentY = space.y + dy

            while (isWithinBounds(currentX, currentY) && getSpaceAt(currentX, currentY).color == opponentColor) {
                moveVal++
                currentX += dx
                currentY += dy
            }

            if (isWithinBounds(currentX, currentY) && getSpaceAt(currentX, currentY).color == playerColor) {
                return moveVal
            }
        }

        return 0
    }

    private fun flipInDirection(space: BoardSpace, dx: Int, dy: Int, playerColor: ReversiColor) {
        space.color = playerColor
        var cx = space.x + dx
        var cy = space.y + dy

        while (getSpaceAt(cx, cy).color != playerColor) {
            getSpaceAt(cx, cy).flip()
            cx += dx
            cy += dy
        }
    }

    fun getNumSpacesForColor(c: ReversiColor) =
            iterator().asSequence().count { space ->
                space.isOwned && space.color == c
            }

    val numberOfEmptySpaces: Int
        get() = iterator().asSequence().count { space -> !space.isOwned }

    fun serialize(): ByteArray {
        val out = ByteArray(64)
        var index = 0
        val i = iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (!s.isOwned)
                out[index++] = 0
            else if (s.color == ReversiColor.LIGHT)
                out[index++] = 1
            else
                out[index++] = 2
        }
        return out
    }

    fun getSpaceNumber(s: BoardSpace): Byte {
        return (s.y * 8 + s.x + 1).toByte()
    }

    fun getBoardSpaceFromNum(n: Int): BoardSpace? {
        return getSpaceAt(n % 8, n / 8)
    }

    fun spaceAt(row: Int, col: Int): BoardSpace {
        return spaces[row][col]
    }

    fun iterator(): BoardIterator {
        return BoardIterator(this)
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
                        else if (it.color == ReversiColor.LIGHT)
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
