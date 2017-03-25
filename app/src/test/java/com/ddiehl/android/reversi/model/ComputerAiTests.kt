package com.ddiehl.android.reversi.model

import com.ddiehl.android.reversi.BaseTest
import com.ddiehl.android.reversi.readBoardFromFile
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ComputerAiTests : BaseTest() {

    @Test
    fun verifyCpuMoveD1_colorIsNull() {
        val board = readBoardFromFile("sample_board.txt")

        val move = ComputerAI.getBestMove_d1(board, ReversiColor.DARK)
        assertNotNull(move)
        assertNull(move!!.color)
    }

    @Test
    fun verifyCpuMoveD3_colorIsNull() {
        val board = readBoardFromFile("sample_board.txt")

        val move = ComputerAI.getBestMove_d3(board, ReversiColor.DARK)
        assertNotNull(move)
        assertNull(move!!.color)
    }
}
