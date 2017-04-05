package com.ddiehl.android.reversi.game

import android.support.annotation.StringRes
import com.ddiehl.android.reversi.model.Board
import com.google.example.games.basegameutils.GameHelper

interface MatchViewMultiPlayer : MatchView {

    fun showSpinner()

    fun dismissSpinner()

    fun toast(msg: String)

    fun toast(@StringRes resId: Int)

    fun toast(@StringRes resId: Int, vararg args: Any)

    fun getGameHelper(): GameHelper

    fun clearBoard()

    fun displaySignInPrompt()

    fun showScore(show: Boolean)

    fun showScore(light: Int, dark: Int)

    fun displayMessage(string: String)

    fun displayMessage(@StringRes resId: Int)

    fun dismissMessage()

    fun displayBoard(board: Board)

    fun showLeaveMatchDialog()

    fun showForfeitMatchDialog()

    fun showCancelMatchDialog()

    fun showForfeitMatchForbiddenAlert()

    fun showAlertDialog(errorTitle: Int, errorMessage: Int)

    fun askForRematch()
}