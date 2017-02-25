package com.ddiehl.android.reversi.view

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.game.Board
import com.jakewharton.rxbinding.view.RxView
import rx.functions.Action1

abstract class MatchFragment : Fragment() {

    @BindView(R.id.match_grid)
    lateinit var mMatchGridView: TableLayout
    @BindView(R.id.score_p1)
    lateinit var mPlayerOneScoreTextView: TextView
    @BindView(R.id.score_p2)
    lateinit var mPlayerTwoScoreTextView: TextView
    @BindView(R.id.p1_waiting_bar)
    lateinit var mP1WaitingBar: ProgressBar
    @BindView(R.id.p2_waiting_bar)
    lateinit var mP2WaitingBar: ProgressBar
    @BindView(R.id.board_panels)
    lateinit var mBoardPanelView: View
    @BindView(R.id.board_panel_new_game)
    lateinit var mStartNewMatchButton: Button
    @BindView(R.id.board_panel_select_game)
    lateinit var mSelectMatchButton: Button
    @BindView(R.id.match_message)
    lateinit var mMatchMessageView: View
    @BindView(R.id.match_message_text)
    lateinit var mMatchMessageTextView: TextView
    @BindView(R.id.match_message_icon_1)
    lateinit var mMatchMessageIcon1: ImageView
    @BindView(R.id.match_message_icon_2)
    lateinit var mMatchMessageIcon2: ImageView

    protected var mDisplayedDialog: Dialog? = null

    protected var mLeftFadeOut: Animation? = null
    protected var mLeftFadeIn: Animation? = null
    protected var mRightFadeOut: Animation? = null
    protected var mRightFadeIn: Animation? = null
    protected var mMatchMessageIcon1Color = false
    protected var mMatchMessageIcon2Color = true

    protected var mBoard: Board? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_reversi, container, false)
        ButterKnife.bind(this, view)

        initMatchGrid(mMatchGridView!!)
        mMatchGridView!!.visibility = View.GONE

        return view
    }

    @OnClick(R.id.board_panel_new_game)
    internal fun onStartNewMatchClicked() {
        startNewMatch()
    }

    @OnClick(R.id.board_panel_select_game)
    internal fun onSelectMatchClicked() {
        selectMatch()
    }

    protected fun initMatchGrid(grid: ViewGroup) {
        for (i in 0..grid.childCount - 1) {
            val row = grid.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)

                RxView.clicks(space)
                        .subscribe(onSpaceClicked(i, j))
            }
        }
    }

    private fun onSpaceClicked(row: Int, col: Int): Action1<Void> {
        return Action1 {
            Log.d(TAG, "Piece clicked @ $row $col")
            handleSpaceClick(row, col)
        }
    }

    protected fun showWaitingIndicator(p1: Boolean, p2: Boolean) {
        mP1WaitingBar!!.visibility = if (p1) View.VISIBLE else View.GONE
        mP2WaitingBar!!.visibility = if (p2) View.VISIBLE else View.GONE
    }

    internal abstract fun startNewMatch()
    internal abstract fun selectMatch()
    internal abstract fun handleSpaceClick(row: Int, col: Int)

    companion object {

        private val TAG = MatchFragment::class.java.simpleName
    }
}
