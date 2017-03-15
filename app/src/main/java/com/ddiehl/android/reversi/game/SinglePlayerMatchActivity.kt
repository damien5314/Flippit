package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.os.Bundle
import android.support.annotation.LayoutRes
import com.ddiehl.android.reversi.*
import com.ddiehl.android.reversi.model.BoardSpace
import com.ddiehl.android.reversi.model.ComputerAI
import com.ddiehl.android.reversi.model.ReversiColor
import com.ddiehl.android.reversi.model.ReversiPlayer
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SinglePlayerMatchActivity : BaseMatchActivity(), MatchView {

    companion object {
        private @LayoutRes val LAYOUT_RES_ID = R.layout.match_activity
    }

    private val m1PSavedState: SinglePlayerSavedState by lazy { SinglePlayerSavedState(this) }
    private val m1PSettings: SinglePlayerSettings by lazy { SinglePlayerSettings(this) }

    private lateinit var mP1: ReversiPlayer
    private lateinit var mP2: ReversiPlayer

    private var mCurrentPlayer: ReversiPlayer? = null
    private var mPlayerWithFirstTurn: ReversiPlayer? = null
    private var mMatchInProgress: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LAYOUT_RES_ID)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mP1 = ReversiPlayer(ReversiColor.LIGHT, getString(R.string.player1_label_default))
        mP2 = ReversiPlayer(ReversiColor.DARK, getString(R.string.player2_label))
        mP1.isCPU(P1_CPU)
        mP2.isCPU(P2_CPU)

        // Hide select match panel for single player
        mMatchFragment.showMatchButtons(true, false)

        // Restore saved state if it exists
        val savedData = m1PSavedState.board
        if (savedData != null) {
            mCurrentPlayer = if (m1PSavedState.currentPlayer) mP1 else mP2
            mPlayerWithFirstTurn = if (m1PSavedState.firstTurn) mP1 else mP2
            mBoard.restoreState(savedData)
            mMatchFragment.updateBoardUi(mBoard)
            mMatchFragment.displayBoard(mBoard)
            updateScoreDisplay()
            mMatchInProgress = true
        } else {
            mMatchInProgress = false
        }
    }

    override fun onResume() {
        super.onResume()

        mP1.name = m1PSettings.playerName
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

    internal fun executeCpuMove() {
        Observable.defer {
            val difficulty = m1PSettings.aiDifficulty
            val move: BoardSpace?
            when (difficulty) {
                1 -> move = ComputerAI.getBestMove_d1(mBoard, mCurrentPlayer!!)
                2 -> move = ComputerAI.getBestMove_d3(mBoard, mCurrentPlayer!!.color)
                else -> move = null
            }
            Observable.just(move)
        }
                .delay(CPU_TURN_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ space ->
                    mBoard.commitPiece(space!!, mCurrentPlayer!!.color)
                    mMatchFragment.updateBoardUi(mBoard, true)
                    calculateMatchState()
                })
    }

    fun calculateMatchState() {
        val opponent = if (mCurrentPlayer === mP1) mP2 else mP1

        // If opponent can make a move, it's his turn
        if (mBoard.hasMove(opponent.color)) {
            mCurrentPlayer = opponent
        }
        // Opponent has no move, keep turn
        else if (mBoard.hasMove(mCurrentPlayer!!.color)) {
            val message = getString(R.string.no_moves, opponent.name)
            toast(message)
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

    fun updateScoreDisplay() {
        var p1c = 0
        var p2c = 0
        val i = mBoard.iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (s.isOwned) {
                if (s.color == ReversiColor.LIGHT) {
                    p1c++
                } else {
                    p2c++
                }
            }
        }
        mP1.score = p1c
        mP2.score = p2c
        mMatchFragment.updateScoreForPlayer(mP1.score, null)
        mMatchFragment.updateScoreForPlayer(null, mP2.score)
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

    override fun handleSpaceClick(row: Int, col: Int) {
        Timber.d("Piece clicked @ $row $col")
        if (mCurrentPlayer!!.isCPU) {
            // Do nothing, it's a CPU's turn
        } else {
            mBoard.requestClaimSpace(row, col, mCurrentPlayer!!.color)
                    .subscribe(onSpaceClaimed(), onSpaceClaimError())
        }
    }

    private fun onSpaceClaimed(): Action1<Boolean> {
        return Action1 {
            mMatchFragment.updateBoardUi(mBoard, true)
            calculateMatchState()
        }
    }

    private fun onSpaceClaimError(): Action1<Throwable> {
        return Action1 { throwable -> toast(throwable.message!!) }
    }

    override fun endMatch() {
        if (mP1.score != mP2.score) {
            val diff = 64 - mP1.score - mP2.score

            if (mP1.score > mP2.score) { // P1 wins
                mP1.score += diff
                mMatchFragment.updateScoreForPlayer(mP1.score, null)
                mMatchFragment.showWinningToast(mP1.color)
            } else { // P2 wins
                mP2.score += diff
                mMatchFragment.updateScoreForPlayer(null, mP2.score)
                mMatchFragment.showWinningToast(mP2.color)
            }
        } else {
            mMatchFragment.showWinningToast(null)
        }
        switchFirstTurn()
        m1PSavedState.clear()
        mMatchInProgress = false
    }

    //endregion


    //region Options menu actions

    override fun onStartNewMatchClicked() {
        mBoard.reset()
        mMatchFragment.displayBoard(mBoard)
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
            mPlayerWithFirstTurn = mP1
        } else {
            mPlayerWithFirstTurn = if (mPlayerWithFirstTurn === mP1) mP2 else mP1
        }
        mCurrentPlayer = mPlayerWithFirstTurn
    }

    override fun clearBoard() {
        mMatchFragment.clearBoard()
    }

    override fun forfeitMatchSelected() {
        TODO("not implemented")
    }

    override fun showAchievements() {
        TODO("not implemented")
    }

    override fun settingsSelected() {
        TODO("not implemented")
    }

    //endregion
}
