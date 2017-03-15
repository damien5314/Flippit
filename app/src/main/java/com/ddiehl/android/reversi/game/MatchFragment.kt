package com.ddiehl.android.reversi.game

import android.app.Dialog
import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.*
import butterknife.bindView
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.delay
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.setListener
import com.ddiehl.android.reversi.toast
import com.jakewharton.rxbinding.view.RxView

class MatchFragment : FrameLayout {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_fragment

        // Delay between animations for the waiting message
        val WAITING_MESSAGE_FADE_DELAY_MS = 2000L

        private val ARG_MULTI_PLAYER = "ARG_MULTI_PLAYER"
        private val PREF_AUTO_SIGN_IN = "PREF_AUTO_SIGN_IN"
    }

    internal val mMatchGridView by bindView<TableLayout>(R.id.match_grid)
    internal val mPlayerOneScore by bindView<TextView>(R.id.score_p1)
    internal val mPlayerTwoScore by bindView<TextView>(R.id.score_p2)
    internal val mPlayerOneLabel by bindView<TextView>(R.id.p1_label)
    internal val mPlayerTwoLabel by bindView<TextView>(R.id.p2_label)
    internal val mBoardPanelView by bindView<View>(R.id.board_panels)
    internal val mStartNewMatchButton by bindView<Button>(R.id.board_panel_new_game)
    internal val mSelectMatchButton by bindView<Button>(R.id.board_panel_select_game)
    internal val mMatchMessageView by bindView<View>(R.id.match_message)
    internal val mMatchMessageTextView by bindView<TextView>(R.id.match_message_text)
    internal val mMatchMessageIcon1 by bindView<ImageView>(R.id.match_message_icon_1)
    internal val mMatchMessageIcon2 by bindView<ImageView>(R.id.match_message_icon_2)

    private var mDisplayedDialog: Dialog? = null

    private val mLeftFadeOut: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadeout) }
    private val mLeftFadeIn: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadein) }
    private val mRightFadeOut: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadeout) }
    private val mRightFadeIn: Animation by lazy { loadAnimation(context, R.anim.waitingmessage_fadein) }
    private var mMatchMessageIcon1Color = false
    private var mMatchMessageIcon2Color = true

    // Activity implements this
    private var mMatchView: MatchView

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
        mMatchView = context as MatchView
    }

    private fun init(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        LayoutInflater.from(context)
                .inflate(LAYOUT_RES_ID, parent as ViewGroup, true)

        mStartNewMatchButton.setOnClickListener { mMatchView.onStartNewMatchClicked() }
        mSelectMatchButton.setOnClickListener { mMatchView.onSelectMatchClicked() }

        mPlayerOneScore.text = 0.toString()
        mPlayerTwoScore.text = 0.toString()

        initMatchGrid(mMatchGridView)
        mMatchGridView.visibility = View.GONE

        initializeWaitingAnimations()
    }


    //region Public API

    fun updateBoardUi(board: Board, animate: Boolean = false) {
        for (i in 0..mMatchGridView.childCount - 1) {
            val row = mMatchGridView.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)
                updateSpace(space, board, i, j, animate)
            }
        }
    }

    fun showMatchButtons(newGame: Boolean, selectGame: Boolean) {
        mSelectMatchButton.visibility = if (newGame) View.VISIBLE else View.GONE
        mSelectMatchButton.visibility = if (selectGame) View.VISIBLE else View.GONE
    }

    fun showScore(light: Int, dark: Int) {
        mPlayerOneScore.text = light.toString()
        mPlayerTwoScore.text = dark.toString()
    }

    //endregion


    private fun initMatchGrid(grid: ViewGroup) {
        for (i in 0..grid.childCount - 1) {
            val row = grid.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)

                RxView.clicks(space)
                        .subscribe({ mMatchView.handleSpaceClick(i, j) })
            }
        }
    }

    private fun updateSpace(view: View, board: Board, row: Int, col: Int, animate: Boolean) {
        val space = board.spaceAt(row, col)
        if (space.color == null) {
            if (view.tag == null || view.tag as Int != 0) {
                view.tag = 0
                if (animate) {
                    animateBackgroundChange(view, R.drawable.board_space_neutral)
                } else {
                    view.setBackgroundResource(R.drawable.board_space_neutral)
                }
            }
        } else {
            when (space.color) {
                ReversiColor.LIGHT ->
                    if (view.tag == null || view.tag as Int != 1) {
                        view.tag = 1
                        if (animate) {
                            animateBackgroundChange(view, R.drawable.board_space_p1)
                        } else {
                            view.setBackgroundResource(R.drawable.board_space_p1)
                        }
                    }
                ReversiColor.DARK ->
                    if (view.tag == null || view.tag as Int != 2) {
                        view.tag = 2
                        if (animate) {
                            animateBackgroundChange(view, R.drawable.board_space_p2)
                        } else {
                            view.setBackgroundResource(R.drawable.board_space_p2)
                        }
                    }
            }
        }
    }

    private fun animateBackgroundChange(view: View, @DrawableRes resId: Int) {
        val fadeOut = loadAnimation(context, R.anim.playermove_fadeout)
        val fadeIn = loadAnimation(context, R.anim.playermove_fadein)

        fadeOut.setListener(onEnd = {
            view.setBackgroundResource(resId)
            view.startAnimation(fadeIn)
        })

        view.startAnimation(fadeOut)
    }

    fun updateScoreForPlayer(light: Int?, dark: Int?) {
        if (light != null) {
            mPlayerOneScore.text = light.toString()
        }
        if (dark != null) {
            mPlayerTwoScore.text = dark.toString()
        }
    }

    fun displayBoard(board: Board) {
        mBoardPanelView.visibility = View.GONE
        mMatchGridView.visibility = View.VISIBLE
        updateBoardUi(board)
    }

    fun showWinningToast(color: ReversiColor?) {
        val text =
                if (color == null) {
                    context.getString(R.string.winner_none)
                } else if (color == ReversiColor.LIGHT) {
                    context.getString(R.string.winner_p1)
                } else if (color == ReversiColor.DARK) {
                    context.getString(R.string.winner_cpu)
                } else throw IllegalStateException("Passed invalid color: " + color.name)
        toast(text, Toast.LENGTH_LONG)
    }

    fun clearBoard() {
        mPlayerTwoScore.text = ""
        mBoardPanelView.visibility = View.VISIBLE
    }

    fun displayMessage(matchMsg: String) {
        mMatchMessageTextView.text = matchMsg
        mMatchMessageView.visibility = View.VISIBLE

        // Start animations for side icons
        if (!mLeftFadeOut.hasStarted() && !mRightFadeOut.hasStarted()) {
            mMatchMessageIcon1.startAnimation(mLeftFadeOut)
            mMatchMessageIcon2.startAnimation(mRightFadeOut)
        }
    }

    fun dismissMessage() {
        mMatchMessageView.visibility = View.INVISIBLE
        mMatchMessageTextView.text = ""
        mLeftFadeOut.cancel()
        mRightFadeOut.cancel()
        mLeftFadeIn.cancel()
        mRightFadeIn.cancel()
    }

    private fun initializeWaitingAnimations() {
        val icon1 = mMatchMessageIcon1
        val icon2 = mMatchMessageIcon2

        icon1.setBackgroundResource(R.drawable.player_icon_p1)
        icon2.setBackgroundResource(R.drawable.player_icon_p2)

        mRightFadeOut.setListener(onEnd = {
            // Flip background resources & start animation
            icon2.setBackgroundResource(
                    if (mMatchMessageIcon2Color) R.drawable.player_icon_p1
                    else R.drawable.player_icon_p2
            )
            mMatchMessageIcon2Color = !mMatchMessageIcon2Color
            icon2.startAnimation(mRightFadeIn)
        })

        mLeftFadeOut.setListener(onEnd = {
            // Flip background resources & start animation
            icon1.setBackgroundResource(
                    if (mMatchMessageIcon1Color) R.drawable.player_icon_p1
                    else R.drawable.player_icon_p2
            )
            mMatchMessageIcon1Color = !mMatchMessageIcon1Color
            icon1.startAnimation(mLeftFadeIn)
        })

        mLeftFadeIn.setListener(onEnd = {
            delay(WAITING_MESSAGE_FADE_DELAY_MS) {
                icon1.startAnimation(mLeftFadeOut)
                icon2.startAnimation(mRightFadeOut)
            }
        })
    }
}
