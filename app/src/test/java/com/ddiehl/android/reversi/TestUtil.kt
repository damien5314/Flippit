package com.ddiehl.android.reversi

import com.ddiehl.android.reversi.model.Board
import org.apache.commons.io.IOUtils

fun readBoardFromFile(filename: String): Board {
    val inputStream = Board::class.java.classLoader.getResourceAsStream(filename)
    val string = stripNewLines(IOUtils.toString(inputStream, "UTF-8"))
    return Board(8, 8).restoreState(string)
}

private fun stripNewLines(string: String): String {
    val builder = StringBuilder()
    for (char in string) {
        if (char != '\n') {
            builder.append(char)
        }
    }
    return builder.toString()
}
