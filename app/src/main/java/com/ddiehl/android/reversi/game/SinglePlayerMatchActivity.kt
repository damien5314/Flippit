package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.annotation.LayoutRes
import com.ddiehl.android.reversi.CPU_TURN_DELAY_MS
import com.ddiehl.android.reversi.P1_CPU
import com.ddiehl.android.reversi.P2_CPU
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.model.*
import com.ddiehl.android.reversi.settings.SettingsActivity
import com.ddiehl.android.reversi.settings.SinglePlayerSettings
import com.google.example.games.basegameutils.GameHelper
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SinglePlayerMatchActivity : BaseMatchActivity(), MatchView {

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

        mMatchFragment.showScore(false)
        mMatchFragment.dismissMessage()

        mPlayerLight = ReversiPlayer(ReversiColor.LIGHT, getString(R.string.player1_label))
        mPlayerDark = ReversiPlayer(ReversiColor.DARK, getString(R.string.player2_label))
        mPlayerLight.isCPU = P1_CPU
        mPlayerDark.isCPU = P2_CPU

        // Hide select match panel for single player
        mMatchFragment.showMatchButtons(true, false)

        // Restore saved state if it exists
        val savedData = m1PSavedState.board
        if (savedData != null) {
            mCurrentPlayer = if (m1PSavedState.currentPlayer) mPlayerLight else mPlayerDark
            mPlayerWithFirstTurn = if (m1PSavedState.firstTurn) mPlayerLight else mPlayerDark
            mBoard.restoreState(savedData)
            mMatchFragment.showScore(true)
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
                        mMatchFragment.updateBoardUi(mBoard, true)
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
        mMatchFragment.updateScoreForPlayer(mPlayerLight.score, null)
        mMatchFragment.updateScoreForPlayer(null, mPlayerDark.score)
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
            mMatchFragment.updateBoardUi(mBoard, true)
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

    fun endMatch() {
        if (mPlayerLight.score != mPlayerDark.score) {
            val diff = 64 - mPlayerLight.score - mPlayerDark.score

            if (mPlayerLight.score > mPlayerDark.score) { // P1 wins
                mPlayerLight.score += diff
                mMatchFragment.updateScoreForPlayer(mPlayerLight.score, null)
                mMatchFragment.displayMessage(getString(R.string.winner_p1))
            } else { // P2 wins
                mPlayerDark.score += diff
                mMatchFragment.updateScoreForPlayer(null, mPlayerDark.score)
                mMatchFragment.displayMessage(getString(R.string.winner_cpu))
            }
        } else {
            mMatchFragment.displayMessage(getString(R.string.winner_tie))
        }
        switchFirstTurn()
        m1PSavedState.clear()
        mMatchInProgress = false
    }

    //endregion


    //region Options menu actions

    override fun onStartNewMatchClicked() {
        mBoard.reset()
        mMatchFragment.showScore(true)
        mMatchFragment.displayBoard(mBoard)
        switchFirstTurn()
        updateScoreDisplay()
        mMatchInProgress = true

        // CPU takes first move if it has turn
        if (mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    override fun onCloseMatchClicked() {
        mMatchFragment.clearBoard()
        mMatchFragment.showMatchButtons(true, false)
    }

    override fun onSelectMatchClicked() {
        // Button is hidden in single player
    }

    override fun onForfeitMatchClicked() {
        throw UnsupportedOperationException()
    }

    override fun onShowAchievementsClicked() {
        throw UnsupportedOperationException()
    }

    override fun onSettingsClicked() {
        val settings = Intent(this, SettingsActivity::class.java)
        startActivityForResult(settings, RC_SETTINGS)
    }

    //endregion

    private fun switchFirstTurn() {
        if (mPlayerWithFirstTurn == null) {
            mPlayerWithFirstTurn = mPlayerLight
        } else {
            mPlayerWithFirstTurn = if (mPlayerWithFirstTurn === mPlayerLight) mPlayerDark else mPlayerLight
        }
        mCurrentPlayer = mPlayerWithFirstTurn
    }


    // TODO Remove these

    override fun getGameHelper(): GameHelper {
        TODO("not implemented")
    }

    override fun clearBoard() {
        TODO("not implemented")
    }

    override fun displaySignInPrompt() {
        TODO("not implemented")
    }

    override fun showScore(show: Boolean) {
        TODO("not implemented")
    }

    override fun showScore(light: Int, dark: Int) {
        TODO("not implemented")
    }

    override fun displayMessage(string: String) {
        TODO("not implemented")
    }

    override fun displayMessage(resId: Int) {
        TODO("not implemented")
    }

    override fun dismissMessage() {
        TODO("not implemented")
    }

    override fun displayBoard(board: Board) {
        TODO("not implemented")
    }

    override fun toast(msg: String) {
        TODO("not implemented")
    }

    override fun toast(resId: Int) {
        TODO("not implemented")
    }

    override fun showLeaveMatchDialog() {
        TODO("not implemented")
    }

    override fun showForfeitMatchDialog() {
        TODO("not implemented")
    }

    override fun showCancelMatchDialog() {
        TODO("not implemented")
    }

    override fun showForfeitMatchForbiddenAlert() {
        TODO("not implemented")
    }

    override fun showAlertDialog(errorTitle: Int, errorMessage: Int) {
        TODO("not implemented")
    }

    override fun askForRematch() {
        TODO("not implemented")
    }

    override fun toast(resId: Int, vararg args: Any) {
        TODO("not implemented")
    }
}
