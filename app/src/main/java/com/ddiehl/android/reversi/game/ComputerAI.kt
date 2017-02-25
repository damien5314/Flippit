package com.ddiehl.android.reversi.game


import java.util.*

object ComputerAI {
    private val TAG = ComputerAI::class.java.simpleName

    private val spaceValues = arrayOf(intArrayOf(99, -8, 8, 6, 6, 8, -8, 99), intArrayOf(-8, -24, -4, -3, -3, -4, -24, -8), intArrayOf(8, -4, 7, 4, 4, 7, -4, 8), intArrayOf(6, -3, 4, 0, 0, 4, -3, 6), intArrayOf(6, -3, 4, 0, 0, 4, -3, 6), intArrayOf(8, -4, 7, 4, 4, 7, -4, 8), intArrayOf(-8, -24, -4, -3, -3, -4, -24, -8), intArrayOf(99, -8, 8, 6, 6, 8, -8, 99))

    /**
     * Finds the space on the board which would capture the most spaces for Player p.
     */
    fun getBestMove_d1(board: Board, p: ReversiPlayer): BoardSpace {
        var best: BoardSpace? = null
        var bestVal = 0
        val i = board.iterator()
        while (i.hasNext()) {
            val space = i.next()
            if (!space.isOwned) {
                val `val` = board.spacesCapturedWithMove(space, p.color)
                if (`val` > bestVal) {
                    best = space
                    bestVal = `val`
                }
            }
        }

        //        if (best != null)
        //            Log.i(TAG, "Best move @(" + best.x + "," + best.y + ") has value of " + bestVal);

        return best!!
    }

    /**
     * Finds the space on the board which would result in a board configuration leaving the
     * opposing player with the least choices.
     */
    fun getBestMove_d2(board: Board, p: ReversiPlayer, o: ReversiPlayer): BoardSpace {
        var best: BoardSpace? = null
        var bestVal = 999
        val i = board.iterator()
        while (i.hasNext()) {
            val space = i.next()
            if (!space.isOwned) {
                if (board.spacesCapturedWithMove(space, p.color) > 0) {
                    // Copy board to identical object
                    val copy = board.copy()
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x(), space.y())!!, p.color)
                    // Count possible moves for Player's opponent
                    val movesOpened = getPossibleMoves(copy, o)
                    if (movesOpened < bestVal) {
                        best = space
                        bestVal = movesOpened
                    }
                }
            }
        }

        //        if (best != null)
        //            Log.i(TAG, "Best move @(" + best.x + "," + best.y + ") reduces "
        //                    + o.getName() + " to " + bestVal + " moves");

        return best!!
    }

    /**
     * Finds space which maximizes space value * number of spaces obtained
     */
    fun getBestMove_d3(board: Board, p: ReversiPlayer, o: ReversiPlayer): BoardSpace {
        val moveValues = HashMap<BoardSpace, Int>()
        val spaceValue_weight = 1
        val spacesCaptured_weight = 0
        val i = board.iterator()
        var space: BoardSpace
        while (i.hasNext()) {
            space = i.next()
            if (!space.isOwned) {
                if (board.spacesCapturedWithMove(space, p.color) > 0) {
                    val moveValue: Int
                    // Copy board to identical object
                    val copy = board.copy()
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x(), space.y())!!, p.color)
                    val movesOpenedForOpponent = getPossibleMoves(copy, o)
                    if (movesOpenedForOpponent == 0)
                        moveValue = 999
                    else
                        moveValue = getSpaceValue(space) * spaceValue_weight + board.spacesCapturedWithMove(space, p.color) * spacesCaptured_weight
                    moveValues.put(space, moveValue)
                }
            }
        }

        // Add all of the moves with the best value to a HashSet
        var bestValue = Integer.MIN_VALUE
        val bestMoves = HashSet<BoardSpace>()
        for (s in moveValues.keys) {
            val `val` = moveValues[s]
            if (`val`!! > bestValue) {
                bestValue = `val`
                bestMoves.clear()
                bestMoves.add(s)
            } else if (`val` == bestValue) {
                bestMoves.add(s)
            }
        }

        // Select a move out of the spaces with the best calculated value
        var best: BoardSpace? = null
        for (s in bestMoves) {
            if (best == null) {
                best = s
            } else {
                if (getSpaceValue(s) > getSpaceValue(best))
                    best = s
            }
        }

        //        if (best != null)
        //            Log.i(TAG, p.getName() + ": " + "Best move @(" + best.x + "," + best.y + "); Value = " + moveValues.get(best));

        return best!!
    }

    private fun getPossibleMoves(board: Board, p: ReversiPlayer): Int {
        var possible = 0
        val i = board.iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (!s.isOwned)
                if (board.spacesCapturedWithMove(s, p.color) > 0)
                    possible++
        }
        return possible
    }

    private fun getSpaceValue(s: BoardSpace): Int {
        return spaceValues[s.y()][s.x()]
    }
}
