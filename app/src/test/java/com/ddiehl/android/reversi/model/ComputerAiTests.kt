package com.ddiehl.android.reversi.model

import org.apache.commons.io.IOUtils
import org.junit.Test

class ComputerAiTests {

    private fun readBoardFromFile(filename: String): Board {
        val inputStream = javaClass.classLoader.getResourceAsStream(filename)
        val string = IOUtils.toString(inputStream, "UTF-8")
        return Board(8, 8).restoreState(string)
    }

    @Test
    fun verifyInvalidMoveFix() {
        val board = readBoardFromFile("sample_board.txt")
    }
}
