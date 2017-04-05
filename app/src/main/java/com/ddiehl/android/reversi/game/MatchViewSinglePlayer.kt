package com.ddiehl.android.reversi.game

import android.support.annotation.StringRes

interface MatchViewSinglePlayer : MatchView {

    fun showSpinner()

    fun dismissSpinner()

    fun toast(msg: String)

    fun toast(@StringRes resId: Int)

    fun toast(@StringRes resId: Int, vararg args: Any)
}
