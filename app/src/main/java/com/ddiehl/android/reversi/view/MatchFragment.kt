package com.ddiehl.android.reversi.view

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.ddiehl.android.reversi.*
import com.ddiehl.android.reversi.model.*
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MatchFragment : Fragment() {

    companion object {
        private val ARG_MULTI_PLAYER = "ARG_MULTI_PLAYER"

        fun newInstance(multiPlayer: Boolean): MatchFragment {
            return MatchFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MULTI_PLAYER, multiPlayer)
                }
            }
        }
    }

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar
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

    private lateinit var mP1: ReversiPlayer
    private lateinit var mP2: ReversiPlayer
    private lateinit var mSavedState: SPSavedState
    private lateinit var mSettings: SPSettings

    private var mCurrentPlayer: ReversiPlayer? = null
    private var mPlayerWithFirstTurn: ReversiPlayer? = null
    private var mMatchInProgress: Boolean = false

    private var mDisplayedDialog: Dialog? = null

    private var mLeftFadeOut: Animation? = null
    private var mLeftFadeIn: Animation? = null
    private var mRightFadeOut: Animation? = null
    private var mRightFadeIn: Animation? = null
    private var mMatchMessageIcon1Color = false
    private var mMatchMessageIcon2Color = true

    private lateinit var mBoard: Board

    private fun isMultiPlayer(): Boolean = arguments.getBoolean(ARG_MULTI_PLAYER)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mSavedState = SPSavedState(context)
        mSettings = SPSettings(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        mP1 = ReversiPlayer(ReversiColor.LIGHT, getString(R.string.player1_label_default))
        mP2 = ReversiPlayer(ReversiColor.DARK, getString(R.string.player2_label))
        mP1.isCPU(P1_CPU)
        mP2.isCPU(P2_CPU)

        mBoard = Board(8, 8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ButterKnife.bind(this, view)

        initMatchGrid(mMatchGridView)
        mMatchGridView.visibility = View.GONE

        if (!isMultiPlayer()) {
            // Hide select match panel for single player
            mSelectMatchButton.visibility = View.GONE
        }

        if (savedMatch) {
            displayBoard()
            updateScoreDisplay()
            mMatchInProgress = true
        } else {
            mMatchInProgress = false
        }

        // Set Action bar
        (activity as AppCompatActivity).setSupportActionBar(mToolbar)
    }

    override fun onResume() {
        super.onResume()
        mP1.name = mSettings.playerName
        if (mMatchInProgress && mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    override fun onPause() {
        super.onPause()

        if (!isMultiPlayer() && mMatchInProgress) {
            mSavedState.save(mBoard, mCurrentPlayer!!, mPlayerWithFirstTurn!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_reversi, container, false)

    @OnClick(R.id.board_panel_new_game)
    internal fun onStartNewMatchClicked() {
        startNewMatch()
    }

    @OnClick(R.id.board_panel_select_game)
    internal fun onSelectMatchClicked() {
        selectMatch()
    }

    private fun initMatchGrid(grid: ViewGroup) {
        for (i in 0..grid.childCount - 1) {
            val row = grid.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)

                RxView.clicks(space)
                        .subscribe({ handleSpaceClick(i, j) })
            }
        }
    }

    private fun showWaitingIndicator(p1: Boolean, p2: Boolean) {
        mP1WaitingBar.visibility = if (p1) View.VISIBLE else View.GONE
        mP2WaitingBar.visibility = if (p2) View.VISIBLE else View.GONE
    }

    val savedMatch: Boolean
        get() {
            val savedData = mSavedState.board
            if (savedData != null) {
                mCurrentPlayer = if (mSavedState.currentPlayer) mP1 else mP2
                mPlayerWithFirstTurn = if (mSavedState.firstTurn) mP1 else mP2

                mBoard = Board.getBoard(mBoard.height, mBoard.width, savedData)
                updateBoardUi(false)
                return true
            }
            return false
        }

    private fun startNewMatch() {
        mBoard.reset()
        displayBoard()
        switchFirstTurn()
        updateScoreDisplay()
        mMatchInProgress = true

        // CPU takes first move if it has turn
        if (mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    private fun selectMatch() {
        // Button is hidden
    }

    private fun handleSpaceClick(row: Int, col: Int) {
        Timber.d("Piece clicked @ $row $col")

        if (mCurrentPlayer!!.isCPU) {
            // do nothing, this isn't a valid state
        } else {
            mBoard.requestClaimSpace(row, col, mCurrentPlayer!!.color)
                    .subscribe(
                            { result ->
                                updateBoardUi(true)
                                calculateMatchState()
                            },
                            { throwable ->
                                Toast.makeText(activity, throwable.message, Toast.LENGTH_SHORT).show()
                            }
                    )
        }
    }

    private fun updateBoardUi(animate: Boolean) {
        for (i in 0..mMatchGridView.childCount - 1) {
            val row = mMatchGridView.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)
                updateSpace(space, mBoard, i, j, animate)
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
                ReversiColor.LIGHT -> if (view.tag == null || view.tag as Int != 1) {
                    view.tag = 1
                    if (animate) {
                        animateBackgroundChange(view, R.drawable.board_space_p1)
                    } else {
                        view.setBackgroundResource(R.drawable.board_space_p1)
                    }
                }
                ReversiColor.DARK -> if (view.tag == null || view.tag as Int != 2) {
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
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.playermove_fadeout)
        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.playermove_fadein)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                view.setBackgroundResource(resId)
                view.startAnimation(fadeIn)
            }
        })

        view.startAnimation(fadeOut)
    }

    private fun switchFirstTurn() {
        if (mPlayerWithFirstTurn == null) {
            mPlayerWithFirstTurn = mP1
        } else {
            mPlayerWithFirstTurn = if (mPlayerWithFirstTurn === mP1) mP2 else mP1
        }
        mCurrentPlayer = mPlayerWithFirstTurn
    }

    fun calculateMatchState() {
        val opponent = if (mCurrentPlayer === mP1) mP2 else mP1

        // If opponent can make a move, it's his turn
        if (mBoard.hasMove(opponent.color)) {
            mCurrentPlayer = opponent
        }
        // Opponent has no move, keep turn
        else if (mBoard.hasMove(mCurrentPlayer!!.color)) {
            Toast.makeText(activity, getString(R.string.no_moves, opponent.name), Toast.LENGTH_SHORT)
                    .show()
        }
        // No moves remaining, end of match
        else {
            updateScoreDisplay()
            endMatch()
            return
        }

        updateScoreDisplay()

        // If the current player is CPU, tell it to execute a move
        if (mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    internal fun executeCpuMove() {
        Observable.defer {
            val difficulty = mSettings.aiDifficulty
            val move: BoardSpace?
            when (difficulty) {
                1 -> {
                    move = ComputerAI.getBestMove_d1(mBoard, mCurrentPlayer!!)
                }
                2 -> {
                    val opponent = if (mCurrentPlayer === mP1) mP2 else mP1
                    move = ComputerAI.getBestMove_d3(mBoard, mCurrentPlayer!!, opponent)
                }
                else -> {
                    move = null
                }
            }

            Observable.just(move)
        }
                .delay(CPU_TURN_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ space ->
                    mBoard.commitPiece(space!!, mCurrentPlayer!!.color)
                    updateBoardUi(true)
                    calculateMatchState()
                })
    }

    fun updateScoreDisplay() {
        var p1c = 0
        var p2c = 0
        val i = mBoard.iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (s.isOwned) {
                if (s.color == ReversiColor.LIGHT)
                    p1c++
                else
                    p2c++
            }
        }
        mP1.score = p1c
        mP2.score = p2c
        updateScoreForPlayer(mP1)
        updateScoreForPlayer(mP2)

        if (mCurrentPlayer === mP1) {
            showWaitingIndicator(false, false)
        } else {
            showWaitingIndicator(false, true)
        }
    }

    fun updateScoreForPlayer(p: ReversiPlayer) {
        (if (p === mP1) mPlayerOneScoreTextView else mPlayerTwoScoreTextView).text = p.score.toString()
    }

    fun endMatch() {
        val winner: ReversiPlayer
        if (mP1.score != mP2.score) {
            winner = if (mP1.score > mP2.score) mP1 else mP2
            showWinningToast(winner)
            val diff = 64 - mP1.score - mP2.score
            winner.score = winner.score + diff
            updateScoreForPlayer(winner)
        } else {
            showWinningToast(null)
        }
        switchFirstTurn()
        mSavedState.clear()
        mMatchInProgress = false
    }

    fun displayBoard() {
        mBoardPanelView.visibility = View.GONE
        mMatchGridView.visibility = View.VISIBLE
        updateBoardUi(false)
    }

    fun showWinningToast(winner: ReversiPlayer?) {
        val text =
                if (winner == null) {
                    getString(R.string.winner_none)
                } else if (winner === mP1) {
                    getString(R.string.winner_p1)
                } else {
                    getString(R.string.winner_cpu)
                }

        Toast.makeText(context, text, Toast.LENGTH_LONG).apply {
            setGravity(Gravity.CENTER, 0, 0)
        }.show()
    }


    //region Options menu

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.single_player, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_match -> {
                startNewMatch()
                return true
            }
            R.id.action_settings -> {
                val settings = Intent(activity, SettingsActivity::class.java)
                settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER)
                startActivity(settings)
                return true
            }
            R.id.action_how_to_play -> {
                val htp = Intent(activity, HowToPlayActivity::class.java)
                startActivity(htp)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //endregion
}
