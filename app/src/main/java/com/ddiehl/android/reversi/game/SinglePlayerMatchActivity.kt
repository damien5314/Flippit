package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.widget.Toolbar
import butterknife.bindView
import com.ddiehl.android.reversi.P1_CPU
import com.ddiehl.android.reversi.P2_CPU
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.model.ReversiPlayer

class SinglePlayerMatchActivity : BaseMatchActivity(), MatchView {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_activity
    }

    private val m1PSavedState: SinglePlayerSavedState by lazy { SinglePlayerSavedState(this) }
    private val m1PSettings: SinglePlayerSettings by lazy { SinglePlayerSettings(this) }

    private lateinit var mP1: ReversiPlayer
    private lateinit var mP2: ReversiPlayer

    internal val mToolbar by bindView<Toolbar>(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_RES_ID)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mP1 = ReversiPlayer(ReversiColor.LIGHT, getString(R.string.player1_label_default))
        mP2 = ReversiPlayer(ReversiColor.DARK, getString(R.string.player2_label))
        mP1.isCPU(P1_CPU)
        mP2.isCPU(P2_CPU)
    }


    //region MatchView

    private val mProgressBar: ProgressDialog by lazy {
        ProgressDialog(this, R.style.ProgressDialog).apply {
            setCancelable(true)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }
    }

    override fun showSpinner() {
        mProgressBar.show()
    }

    override fun dismissSpinner() {
        mProgressBar.dismiss()
    }

    //endregion
}
