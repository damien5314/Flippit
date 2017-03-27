package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.view.View
import com.ddiehl.android.reversi.*
import com.ddiehl.android.reversi.model.BoardSpace
import com.ddiehl.android.reversi.model.ComputerAI
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.model.ReversiPlayer
import com.ddiehl.android.reversi.settings.SettingsActivity
import com.ddiehl.android.reversi.settings.SinglePlayerSettings
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SinglePlayerMatchActivity : BaseMatchActivity(), IMatchView {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_activity

        private val RC_SETTINGS = 1006
    }

    private val m1PSavedState: SinglePlayerSavedState by lazy { SinglePlayerSavedState(this) }
    private val m1PSettings: SinglePlayerSettings by lazy { SinglePlayerSettings(this) }

    private lateinit var mPlayerLight: ReversiPlayer
    private lateinit var mPlayerDark: ReversiPlayer

    private var mCurrentPlayer: ReversiPlayer? = null
    private var mPlayerWithFirstTurn: ReversiPlayer? = null
    private var mMatchInProgress: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_RES_ID)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mMatchView.mMatchMessageView.visibility = View.INVISIBLE

        mPlayerLight = ReversiPlayer(ReversiColor.LIGHT, getString(R.string.player1_label))
        mPlayerDark = ReversiPlayer(ReversiColor.DARK, getString(R.string.player2_label))
        mPlayerLight.isCPU = P1_CPU
        mPlayerDark.isCPU = P2_CPU

        // Hide select match panel for single player
        mMatchView.showMatchButtons(true, false)

        // Restore saved state if it exists
        val savedData = m1PSavedState.board
        if (savedData != null) {
            mCurrentPlayer = if (m1PSavedState.currentPlayer) mPlayerLight else mPlayerDark
            mPlayerWithFirstTurn = if (m1PSavedState.firstTurn) mPlayerLight else mPlayerDark
            mBoard.restoreState(savedData)
            mMatchView.updateBoardUi(mBoard)
            mMatchView.displayBoard(mBoard)
            updateScoreDisplay()
            mMatchInProgress = true
        } else {
            mMatchInProgress = false
        }
    }

    override fun onResume() {
        super.onResume()

        mPlayerLight.name = m1PSettings.playerName
        if (mMatchInProgress && mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    override fun onPause() {
        if (mMatchInProgress) {
            m1PSavedState.save(mBoard, mCurrentPlayer!!, mPlayerWithFirstTurn!!)
        }

        super.onPause()
    }

    private fun executeCpuMove() {
        getCpuMove()
                .delay(CPU_TURN_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ space ->
                    if (space == null) {
                        val opponent = if (mCurrentPlayer === mPlayerLight) mPlayerDark else mPlayerLight
                        toast(getString(R.string.no_moves, opponent.name))
                    } else {
                        mBoard.commitPiece(space, mCurrentPlayer!!.color)
                        mMatchView.updateBoardUi(mBoard, true)
                        calculateMatchState()
                    }
                })
    }

    private fun getCpuMove(): Observable<BoardSpace?> {
        return Observable.defer {
            val difficulty = m1PSettings.aiDifficulty
            val move: BoardSpace? =
                    when (difficulty) {
                        AiDifficulty.EASY -> ComputerAI.getBestMove_d1(mBoard, mCurrentPlayer!!.color)
                        AiDifficulty.HARD -> ComputerAI.getBestMove_d3(mBoard, mCurrentPlayer!!.color)
                        else -> throw IllegalArgumentException("Invalid difficulty: " + difficulty.name)
                    }
            Observable.just(move)
        }
    }

    fun calculateMatchState() {
        val opponent = if (mCurrentPlayer === mPlayerLight) mPlayerDark else mPlayerLight

        val playerHasMove = mBoard.hasMove(mCurrentPlayer!!.color)
        val opponentHasMove = mBoard.hasMove(opponent.color)

        if (playerHasMove || opponentHasMove) {
            // If opponent can make a move, it's his turn
            if (opponentHasMove) {
                mCurrentPlayer = opponent
            }
            // Opponent has no move, keep turn
            else if (playerHasMove) {
                val message = getString(R.string.no_moves, opponent.name)
                toast(message)
            }

            updateScoreDisplay()

            // If the current player is CPU, tell it to execute a move
            if (mCurrentPlayer!!.isCPU) {
                executeCpuMove()
            }
        }
        // No moves remaining, end of match
        else {
            updateScoreDisplay()
            endMatch()
        }
    }

    fun updateScoreDisplay() {
        mPlayerLight.score = mBoard.count { it.isOwned && it.color == ReversiColor.LIGHT }
        mPlayerDark.score = mBoard.count { it.isOwned && it.color == ReversiColor.DARK }
        mMatchView.updateScoreForPlayer(mPlayerLight.score, null)
        mMatchView.updateScoreForPlayer(null, mPlayerDark.score)
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

    override fun onSpaceClick(row: Int, col: Int) {
        Timber.d("Space clicked @ $row $col")
        if (mCurrentPlayer!!.isCPU) {
            // Do nothing, it's a CPU's turn
        } else {
            mBoard.requestClaimSpace(row, col, mCurrentPlayer!!.color)
                    .subscribe(onSpaceClaimed(), onSpaceClaimError())
        }
    }

    private fun onSpaceClaimed(): Action1<Boolean> {
        return Action1 {
            mMatchView.updateBoardUi(mBoard, true)
            calculateMatchState()
        }
    }

    private fun onSpaceClaimError(): Action1<Throwable> {
        return Action1 { throwable ->
            val msg = throwable.message
            if (msg != null) {
                Timber.d(throwable.message)
            }
        }
    }

    // TODO
    // We could probably refactor the below to use refreshUi(), but making a Player abstraction
    // usable in both activity subclasses is probably a prerequisite

    override fun endMatch() {
        if (mPlayerLight.score != mPlayerDark.score) {
            val diff = 64 - mPlayerLight.score - mPlayerDark.score

            if (mPlayerLight.score > mPlayerDark.score) { // P1 wins
                mPlayerLight.score += diff
                mMatchView.updateScoreForPlayer(mPlayerLight.score, null)
                mMatchView.displayMessage(getString(R.string.winner_p1))
            } else { // P2 wins
                mPlayerDark.score += diff
                mMatchView.updateScoreForPlayer(null, mPlayerDark.score)
                mMatchView.displayMessage(getString(R.string.winner_cpu))
            }
        } else {
            mMatchView.displayMessage(getString(R.string.winner_tie))
        }
        switchFirstTurn()
        m1PSavedState.clear()
        mMatchInProgress = false
    }

    //endregion


    //region Options menu actions

    override fun onStartNewMatchClicked() {
        mBoard.reset()
        mMatchView.displayBoard(mBoard)
        switchFirstTurn()
        updateScoreDisplay()
        mMatchInProgress = true

        // CPU takes first move if it has turn
        if (mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    override fun onSelectMatchClicked() {
        // Button is hidden in single player
    }

    private fun switchFirstTurn() {
        if (mPlayerWithFirstTurn == null) {
            mPlayerWithFirstTurn = mPlayerLight
        } else {
            mPlayerWithFirstTurn = if (mPlayerWithFirstTurn === mPlayerLight) mPlayerDark else mPlayerLight
        }
        mCurrentPlayer = mPlayerWithFirstTurn
    }

    override fun forfeitMatchSelected() {
        throw UnsupportedOperationException()
    }

    override fun showAchievements() {
        throw UnsupportedOperationException()
    }

    override fun settingsSelected() {
        val settings = Intent(this, SettingsActivity::class.java)
        startActivityForResult(settings, RC_SETTINGS)
    }

    //endregion
}
