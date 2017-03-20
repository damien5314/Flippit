package com.ddiehl.android.reversi.game

interface IMatchView {

    fun showSpinner()

    fun dismissSpinner()

    fun onStartNewMatchClicked()

    fun onSelectMatchClicked()

    fun onSpaceClick(row: Int, col: Int)

    fun endMatch()
}
