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
    fun getBestMove_d1(board: Board, player: ReversiPlayer): BoardSpace? {
        var best: BoardSpace? = null
        var bestValue = 0

        val iterator = board.iterator()

        while (iterator.hasNext()) {
            val space = iterator.next()
            if (!space.isOwned) {
                val value = board.spacesCapturedWithMove(space, player.color)
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
    fun getBestMove_d2(board: Board, player: ReversiPlayer, opponent: ReversiPlayer): BoardSpace {
        var best: BoardSpace? = null
        var bestVal = 999
        val i = board.iterator()
        while (i.hasNext()) {
            val space = i.next()
            if (!space.isOwned) {
                if (board.spacesCapturedWithMove(space, player.color) > 0) {
                    // Copy board to identical object
                    val copy = board.copy()
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x, space.y), player.color)
                    // Count possible moves for Player's opponent
                    val movesOpened = getPossibleMoves(copy, opponent)
                    if (movesOpened < bestVal) {
                        best = space
                        bestVal = movesOpened
                    }
                }
            }
        }

        return best!!
    }

    /**
     * Finds space which maximizes space value * number of spaces obtained
     */
    fun getBestMove_d3(board: Board, p: ReversiPlayer, o: ReversiPlayer): BoardSpace {
        val moveValues = HashMap<BoardSpace, Int>()

        val spaceValueWeight = 1
        val spacesCapturedWeight = 0 // Not factoring in captured spaces at the moment

        for (space in board.iterator()) {
            if (!space.isOwned) {
                if (board.spacesCapturedWithMove(space, p.color) > 0) {
                    val moveValue: Int
                    // Copy board to identical object
                    val copy = board.copy()
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x, space.y), p.color)
                    val movesOpenedForOpponent = getPossibleMoves(copy, o)
                    if (movesOpenedForOpponent == 0) {
                        moveValue = 999
                    } else {
                        moveValue =
                                getSpaceValue(space) * spaceValueWeight +
                                        board.spacesCapturedWithMove(space, p.color) * spacesCapturedWeight
                    }
                    moveValues.put(space, moveValue)
                }
            }
        }

        // Add all of the moves with the best value to a HashSet
        var bestValue = Integer.MIN_VALUE
        val bestMoves = HashSet<BoardSpace>()
        for (s in moveValues.keys) {
            val value = moveValues[s]
            if (value!! > bestValue) {
                bestValue = value
                bestMoves.clear()
                bestMoves.add(s)
            } else if (value == bestValue) {
                bestMoves.add(s)
            }
        }

        // Select a move out of the spaces with the best calculated value
        return bestMoves.maxBy { getSpaceValue(it) }!!
    }

    private fun getPossibleMoves(board: Board, p: ReversiPlayer): Int {
        var possible = 0

        for (space in board.iterator()) {
            if (!space.isOwned) {
                if (board.spacesCapturedWithMove(space, p.color) > 0) possible++
            }
        }

        return possible
    }

    private fun getSpaceValue(s: BoardSpace): Int {
        return spaceValues[s.y][s.x]
    }
}
