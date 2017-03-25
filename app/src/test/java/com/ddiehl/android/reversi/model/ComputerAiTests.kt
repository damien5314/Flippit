package com.ddiehl.android.reversi.model

import com.ddiehl.android.reversi.BaseTest
import com.ddiehl.android.reversi.readBoardFromFile
import org.junit.Assert.*
import org.junit.Test

class ComputerAiTests : BaseTest() {

    @Test
    fun verifyCpuMoveD1_colorIsNull() {
        val board = readBoardFromFile("board_case_1.txt")
        val move = ComputerAI.getBestMove_d1(board, ReversiColor.DARK)

        assertNotNull(move)
        assertNull(move!!.color)
    }

    @Test
    fun verifyCpuMoveD3_colorIsNull() {
        val board = readBoardFromFile("board_case_1.txt")
        val move = ComputerAI.getBestMove_d3(board, ReversiColor.DARK)

        assertNotNull(move)
        assertNull(move!!.color)
    }

    @Test
    fun verifyCpuMoveD3_openCorner_cornerReturned() {
        val board = readBoardFromFile("board_open_corner.txt")
        val move = ComputerAI.getBestMove_d3(board, ReversiColor.DARK)

        assertNotNull(move)
        assertEquals(BoardSpace(0, 0), move)
    }

    @Test
    fun verifyCpuMoveD3_leavePlayerWithNoMoves_spaceReturned() {
        // Create board with open corner move, but a different space would leave player with no moves
        val board = readBoardFromFile("board_player_no_moves.txt")
        val move = ComputerAI.getBestMove_d3(board, ReversiColor.DARK)

        assertNotNull(move)
        assertEquals(BoardSpace(3, 1), move)
    }
}
