package com.ddiehl.android.reversi.view

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.DrawableRes
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import butterknife.BindView
import com.ddiehl.android.reversi.CPU_TURN_DELAY_MS
import com.ddiehl.android.reversi.P1_CPU
import com.ddiehl.android.reversi.P2_CPU
import com.ddiehl.android.reversi.R
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

        private val PREF_PLAYER_NAME = "pref_player_name"
        private val PREF_AI_DIFFICULTY = "pref_ai_difficulty"
        private val PREF_CURRENT_PLAYER = "pref_currentPlayer"
        private val PREF_FIRST_TURN = "pref_firstTurn"
        private val PREF_BOARD_STATE = "pref_boardState"
    }

    private var mP1: ReversiPlayer? = null
    private var mP2: ReversiPlayer? = null
    private var mCurrentPlayer: ReversiPlayer? = null
    private var mPlayerWithFirstTurn: ReversiPlayer? = null
    private var mMatchInProgress: Boolean = false

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(activity, R.xml.preferences, false)

        mP1 = ReversiPlayer(LIGHT, getString(R.string.player1_label_default))
        mP2 = ReversiPlayer(DARK, getString(R.string.player2_label))
        mP1!!.isCPU(P1_CPU)
        mP2!!.isCPU(P2_CPU)

        mBoard = Board(8, 8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide select match panel for single player
        mSelectMatchButton!!.visibility = View.GONE

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
        mP1!!.name = playerName
        if (mMatchInProgress && mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    override fun onPause() {
        super.onPause()

        if (mMatchInProgress) {
            saveMatchToPrefs()
        }
    }

    val savedMatch: Boolean
        get() {
            val sp = PreferenceManager.getDefaultSharedPreferences(activity)
            if (sp.contains(PREF_CURRENT_PLAYER)
                    && sp.contains(PREF_FIRST_TURN)
                    && sp.contains(PREF_BOARD_STATE)) {
                mCurrentPlayer = if (sp.getBoolean(PREF_CURRENT_PLAYER, true)) mP1 else mP2
                mPlayerWithFirstTurn = if (sp.getBoolean(PREF_FIRST_TURN, true)) mP1 else mP2

                val savedData = sp.getString(PREF_BOARD_STATE, "")
                mBoard = Board(mBoard!!.height, mBoard!!.width, savedData)
                updateBoardUi(false)
                return true
            }
            return false
        }

    fun saveMatchToPrefs() {
        val bytes = mBoard!!.serialize()
        val out = StringBuilder()
        for (b in bytes)
            out.append(b.toInt())

        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                .putBoolean(PREF_CURRENT_PLAYER, mCurrentPlayer === mP1)
                .putBoolean(PREF_FIRST_TURN, mPlayerWithFirstTurn === mP1)
                .putString(PREF_BOARD_STATE, out.toString())
                .apply()
    }

    private val playerName: String
        get() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            return prefs.getString(PREF_PLAYER_NAME, getString(R.string.player1_label_default))
        }

    public override fun startNewMatch() {
        mBoard!!.reset()
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
            mBoard!!.requestClaimSpace(row, col, mCurrentPlayer!!.color)
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
        for (i in 0..mMatchGridView!!.childCount - 1) {
            val row = mMatchGridView!!.getChildAt(i) as ViewGroup
            for (j in 0..row.childCount - 1) {
                val space = row.getChildAt(j)
                updateSpace(space, mBoard!!, i, j, animate)
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
        if (mBoard!!.hasMove(opponent!!.color)) { // If opponent can make a move, it's his turn
            mCurrentPlayer = opponent
        } else if (mBoard!!.hasMove(mCurrentPlayer!!.color)) { // Opponent has no move, keep turn
            Toast.makeText(activity, getString(R.string.no_moves) + opponent!!.name, Toast.LENGTH_SHORT).show()
        } else { // No moves remaining, end of match
            updateScoreDisplay()
            endMatch()
            return
        }
        updateScoreDisplay()
        if (mCurrentPlayer!!.isCPU) {
            executeCpuMove()
        }
    }

    internal fun executeCpuMove() {
        Observable.defer {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val difficulty = prefs.getString(PREF_AI_DIFFICULTY, "")
            val move: BoardSpace?
            when (difficulty) {
                "1" -> move = ComputerAI.getBestMove_d1(mBoard!!, mCurrentPlayer!!)
                "2" -> move = ComputerAI.getBestMove_d3(mBoard!!, mCurrentPlayer!!, if (mCurrentPlayer === mP1) mP2!! else mP1!!)
                else -> move = null
            }

            Observable.just(move)
        }
                .delay(CPU_TURN_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ space ->
                    Log.d(TAG, "CPU move updated")
                    mBoard!!.commitPiece(space!!, mCurrentPlayer!!.color)
                    updateBoardUi(true)
                    calculateMatchState()
                })
    }

    fun updateScoreDisplay() {
        var p1c = 0
        var p2c = 0
        val i = mBoard!!.iterator()
        while (i.hasNext()) {
            val s = i.next()
            if (s.isOwned) {
                if (s.color == LIGHT)
                    p1c++
                else
                    p2c++
            }
        }
        mP1!!.score = p1c
        mP2!!.score = p2c
        updateScoreForPlayer(mP1!!)
        updateScoreForPlayer(mP2!!)

        if (mCurrentPlayer === mP1) {
            showWaitingIndicator(false, false)
        } else {
            showWaitingIndicator(false, true)
        }
    }

    fun updateScoreForPlayer(p: ReversiPlayer) {
        (if (p === mP1) mPlayerOneScoreTextView else mPlayerTwoScoreTextView)!!.text = p.score.toString()
    }

    fun endMatch() {
        val winner: ReversiPlayer
        if (mP1!!.score != mP2!!.score) {
            winner = if (mP1!!.score > mP2!!.score) mP1!! else mP2!!
            showWinningToast(winner)
            val diff = 64 - mP1!!.score - mP2!!.score
            winner.score = winner.score + diff
            updateScoreForPlayer(winner)
        } else {
            showWinningToast(null)
        }
        switchFirstTurn()
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .remove(PREF_CURRENT_PLAYER)
                .remove(PREF_FIRST_TURN)
                .remove(PREF_BOARD_STATE)
                .apply()
        mMatchInProgress = false
    }

    fun displayBoard() {
        mBoardPanelView!!.visibility = View.GONE
        mMatchGridView!!.visibility = View.VISIBLE
        updateBoardUi(false)
    }

    fun showWinningToast(winner: ReversiPlayer?) {
        if (winner != null) {
            val toast: Toast
            if (winner === mP1) {
                toast = Toast.makeText(activity, getString(R.string.winner_p1), Toast.LENGTH_LONG)
            } else {
                toast = Toast.makeText(activity, getString(R.string.winner_cpu), Toast.LENGTH_LONG)
            }
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        } else { // You tied
            val t = Toast.makeText(activity, getString(R.string.winner_none), Toast.LENGTH_LONG)
            t.setGravity(Gravity.CENTER, 0, 0)
            t.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.single_player, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
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
