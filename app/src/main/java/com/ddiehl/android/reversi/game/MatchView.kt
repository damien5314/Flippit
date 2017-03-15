package com.ddiehl.android.reversi.game

interface MatchView {
    fun showSpinner()
    fun dismissSpinner()
    fun onStartNewMatchClicked()
    fun onSelectMatchClicked()
    fun handleSpaceClick(row: Int, col: Int)
}
