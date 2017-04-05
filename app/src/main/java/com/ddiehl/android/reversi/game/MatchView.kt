package com.ddiehl.android.reversi.game

interface MatchView {

    fun onStartNewMatchClicked()

    fun onSelectMatchClicked()

    fun onSpaceClick(row: Int, col: Int)
}
