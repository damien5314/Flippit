package com.ddiehl.android.reversi.game

import android.support.annotation.StringRes
import com.ddiehl.android.reversi.model.Board
import com.google.example.games.basegameutils.GameHelper

interface IMatchView {

    fun getGameHelper(): GameHelper

    fun clearBoard()

    fun showSpinner()

    fun dismissSpinner()

    fun onStartNewMatchClicked()

    fun onSelectMatchClicked()

    fun onSpaceClick(row: Int, col: Int)

    fun displaySignInPrompt()

    fun showScore(show: Boolean)

    fun showScore(light: Int, dark: Int)

    fun displayMessage(string: String)

    fun displayMessage(@StringRes resId: Int)

    fun dismissMessage()

    fun displayBoard(board: Board)

    fun toast(msg: String)

    fun toast(@StringRes resId: Int)

    fun showLeaveMatchDialog()

    fun showForfeitMatchDialog()

    fun showCancelMatchDialog()

    fun showForfeitMatchForbiddenAlert()

    fun showAlertDialog(errorTitle: Int, errorMessage: Int)

    fun askForRematch()

    fun toast(@StringRes resId: Int, vararg args: Any)
}
