package com.ddiehl.android.reversi.model

import java.util.*

object ComputerAI {

    private val spaceValues = arrayOf(
            intArrayOf(99, -8, 8, 6, 6, 8, -8, 99),
            intArrayOf(-8, -24, -4, -3, -3, -4, -24, -8),
            intArrayOf(8, -4, 7, 4, 4, 7, -4, 8),
            intArrayOf(6, -3, 4, 0, 0, 4, -3, 6),
            intArrayOf(6, -3, 4, 0, 0, 4, -3, 6),
            intArrayOf(8, -4, 7, 4, 4, 7, -4, 8),
            intArrayOf(-8, -24, -4, -3, -3, -4, -24, -8),
            intArrayOf(99, -8, 8, 6, 6, 8, -8, 99)
    )

    /**
     * Finds the space on the board which would capture the most spaces for Player p.
     */
    fun getBestMove_d1(board: Board, color: ReversiColor): BoardSpace? {
        var best: BoardSpace? = null
        var bestValue = 0

        board.forEach { space ->
            if (!space.isOwned) {
                val value = board.spacesCapturedWithMove(space, color)
                if (value > bestValue) {
                    best = space
                    bestValue = value
                }
            }
        }

        return best
    }

    /**
     * Finds the space on the board which would result in a board configuration leaving the
     * opposing player with the least choices.
     */
    fun getBestMove_d2(board: Board, color: ReversiColor): BoardSpace? {
        var best: BoardSpace? = null
        var bestVal = 999

        board.forEach { space ->
            if (!space.isOwned) {
                if (board.spacesCapturedWithMove(space, color) > 0) {
                    // Copy board to identical object
                    val copy = board.copy()
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x, space.y), color)
                    // Count possible moves for Player's opponent
                    val movesOpened = getPossibleMoves(copy, color.opposite())
                    if (movesOpened < bestVal) {
                        best = space
                        bestVal = movesOpened
                    }
                }
            }
        }

        return best
    }

    /**
     * Finds space which maximizes space value * number of spaces obtained
     */
    fun getBestMove_d3(board: Board, color: ReversiColor): BoardSpace? {
        val moveValues = HashMap<BoardSpace, Int>()

        val spaceValueWeight = 1
        val spacesCapturedWeight = 0 // Not factoring in captured spaces at the moment

        board.forEach { space ->
            if (board.spacesCapturedWithMove(space, color) > 0) {
                val moveValue: Int
                // Copy board to identical object
                val copy = board.copy()
                // Play move on copied board object
                copy.commitPiece(copy.getSpaceAt(space.x, space.y), color)
                val movesOpenedForOpponent = getPossibleMoves(copy, color.opposite())
                if (movesOpenedForOpponent == 0) {
                    moveValue = 999
                } else {
                    moveValue =
                            getSpaceValue(space) * spaceValueWeight +
                                    board.spacesCapturedWithMove(space, color) * spacesCapturedWeight
                }
                moveValues.put(space, moveValue)
            }
        }

        // Add all of the moves with the best value to a HashSet
        var bestValue = Integer.MIN_VALUE
        val bestMoves = HashSet<BoardSpace>()
        for (space in moveValues.keys) {
            val value = moveValues[space]
            if (value!! > bestValue) {
                bestValue = value
                bestMoves.clear()
                bestMoves.add(space)
            } else if (value == bestValue) {
                bestMoves.add(space)
            }
        }

        // Select a move out of the spaces with the best calculated value
        return bestMoves.maxBy { getSpaceValue(it) }
    }

    private fun getPossibleMoves(board: Board, color: ReversiColor): Int {
        return board.count { space ->
            !space.isOwned && board.spacesCapturedWithMove(space, color) > 0
        }
    }

    private fun getSpaceValue(s: BoardSpace): Int {
        return spaceValues[s.y][s.x]
    }
}
