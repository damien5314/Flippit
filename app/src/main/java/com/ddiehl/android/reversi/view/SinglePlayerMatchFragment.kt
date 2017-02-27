package com.ddiehl.android.reversi.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import butterknife.BindView
import com.ddiehl.android.reversi.*
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.model.BoardSpace
import com.ddiehl.android.reversi.model.ComputerAI
import com.ddiehl.android.reversi.model.ReversiColor.DARK
import com.ddiehl.android.reversi.model.ReversiColor.LIGHT
import com.ddiehl.android.reversi.model.ReversiPlayer
import com.ddiehl.android.reversi.view.SettingsActivity.Companion.EXTRA_SETTINGS_MODE
import com.ddiehl.android.reversi.view.SettingsActivity.Companion.SETTINGS_MODE_SINGLE_PLAYER
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SinglePlayerMatchFragment : MatchFragment() {

    companion object {
        private val TAG = SinglePlayerMatchFragment::class.java.simpleName
        private val SP_STATE = "SP_STATE"
    }

    private lateinit var mP1: ReversiPlayer
    private lateinit var mP2: ReversiPlayer
    private lateinit var mSavedState: SPSavedState
    private lateinit var mSettings: SPSettings

    private var mCurrentPlayer: ReversiPlayer? = null
    private var mPlayerWithFirstTurn: ReversiPlayer? = null
    private var mMatchInProgress: Boolean = false

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val prefs = context.getSharedPreferences(SP_STATE, Context.MODE_PRIVATE)
        mSavedState = SPSavedState(prefs)
        mSettings = SPSettings(prefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mP1 = ReversiPlayer(LIGHT, getString(R.string.player1_label_default))
        mP2 = ReversiPlayer(DARK, getString(R.string.player2_label))
        mP1.isCPU(P1_CPU)
        mP2.isCPU(P2_CPU)

        mBoard = Board(8, 8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide select match panel for single player
        mSelectMatchButton.visibility = View.GONE

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

        if (mMatchInProgress) {
            mSavedState.save(mBoard, mCurrentPlayer!!, mPlayerWithFirstTurn!!)
        }
    }

    val savedMatch: Boolean
        get() {
            val savedData = mSavedState.board
            if (savedData != null) {
                mCurrentPlayer = if (mSavedState.currentPlayer) mP1 else mP2
                mPlayerWithFirstTurn = if (mSavedState.firstTurn) mP1 else mP2

                mBoard = Board(mBoard.height, mBoard.width, savedData)
                updateBoardUi(false)
                return true
            }
            return false
        }

    public override fun startNewMatch() {
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

    public override fun selectMatch() {
        // Button is hidden
    }

    override fun handleSpaceClick(row: Int, col: Int) {
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
                LIGHT -> if (view.tag == null || view.tag as Int != 1) {
                    view.tag = 1
                    if (animate) {
                        animateBackgroundChange(view, R.drawable.board_space_p1)
                    } else {
                        view.setBackgroundResource(R.drawable.board_space_p1)
                    }
                }
                DARK -> if (view.tag == null || view.tag as Int != 2) {
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
                "1" -> move = ComputerAI.getBestMove_d1(mBoard, mCurrentPlayer!!)
                "2" -> move = ComputerAI.getBestMove_d3(mBoard, mCurrentPlayer!!, if (mCurrentPlayer === mP1) mP2 else mP1)
                else -> move = null
            }

            Observable.just(move)
        }
                .delay(CPU_TURN_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ space ->
                    Log.d(TAG, "CPU move updated")
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
                if (s.color == LIGHT)
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.single_player, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_match -> {
                startNewMatch()
                return true
            }
            R.id.action_settings -> {
                val settings = Intent(activity, SettingsActivity::class.java)
                settings.putExtra(EXTRA_SETTINGS_MODE, SETTINGS_MODE_SINGLE_PLAYER)
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
}
