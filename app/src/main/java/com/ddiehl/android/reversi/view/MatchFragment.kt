package com.ddiehl.android.reversi.view

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
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
import com.ddiehl.android.reversi.multiplayer.AchievementManager
import com.ddiehl.android.reversi.multiplayer.Achievements
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.android.gms.games.GamesStatusCodes
import com.google.android.gms.games.multiplayer.Multiplayer
import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.ParticipantResult
import com.google.android.gms.games.multiplayer.realtime.RoomConfig
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer
import com.google.android.gms.plus.Plus
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class MatchFragment : Fragment(),
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        OnTurnBasedMatchUpdateReceivedListener {

    companion object {
        private val ARG_MULTI_PLAYER = "ARG_MULTI_PLAYER"
        private val PREF_AUTO_SIGN_IN = "PREF_AUTO_SIGN_IN"

        private val RC_RESOLVE_ERROR = 1001
        private val RC_VIEW_MATCHES = 1002
        private val RC_SELECT_PLAYERS = 1003
        private val RC_SHOW_ACHIEVEMENTS = 1004
        private val RC_SETTINGS = 1005
        private val MAXIMUM_SCORE = 64 // FIXME: Calculate this dynamically

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
    private lateinit var m1PSavedState: SPSavedState
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


    //region Multi Player fragment fields

    private val mProgressBar: ProgressDialog by lazy {
        ProgressDialog(activity, R.style.ProgressDialog).apply {
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }
    }

    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mAchievementManager: AchievementManager

    private var mMatch: TurnBasedMatch? = null
    private var mPlayer: Participant? = null
    private var mOpponent: Participant? = null
    private var mLightPlayer: Participant? = null
    private var mDarkPlayer: Participant? = null
    private var mMatchData: ByteArray? = null
    private var mLightScore: Int = 0
    private var mDarkScore: Int = 0

    private var mSignInOnStart = true
    private var mSignOutOnConnect = false
    private var mResolvingError = false
    private var mUpdatingMatch = false
    private var mIsSignedIn = false

    private var mHandler: Handler? = null
    private val mQueuedMoves: MutableList<BoardSpace> = ArrayList()

    private var mQueuedAction: QueuedAction? = null

    private enum class QueuedAction {
        NewMatch, SelectMatch, ShowAchievements, ForfeitMatch
    }

    //endregion


    private fun singlePlayer(f: () -> Unit) {
        if (!arguments.getBoolean(ARG_MULTI_PLAYER)) {
            f.invoke()
        }
    }

    private fun multiPlayer(f: () -> Unit) {
        if (arguments.getBoolean(ARG_MULTI_PLAYER)) {
            f.invoke()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        m1PSavedState = SPSavedState(context)
        mSettings = SPSettings(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        singlePlayer {
            mP1 = ReversiPlayer(ReversiColor.LIGHT, getString(R.string.player1_label_default))
            mP2 = ReversiPlayer(ReversiColor.DARK, getString(R.string.player2_label))
            mP1.isCPU(P1_CPU)
            mP2.isCPU(P2_CPU)

            mBoard = Board(8, 8)
        }

        multiPlayer {
            mBoard = Board(8, 8)
            mSignInOnStart = autoConnectPreference
            mHandler = Handler()

            // Initialize Games API client
            val client = buildGoogleApiClient()
            mAchievementManager = AchievementManager.get(client)
            mGoogleApiClient = client

            if (activity.intent.hasExtra(Multiplayer.EXTRA_TURN_BASED_MATCH)) {
                mMatch = activity.intent.getParcelableExtra<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ButterKnife.bind(this, view)

        initMatchGrid(mMatchGridView)
        mMatchGridView.visibility = View.GONE

        // Set Action bar
        (activity as AppCompatActivity).setSupportActionBar(mToolbar)

        singlePlayer {
            // Hide select match panel for single player
            mSelectMatchButton.visibility = View.GONE

            // Restore saved state if it exists
            val savedData = m1PSavedState.board
            if (savedData != null) {
                mCurrentPlayer = if (m1PSavedState.currentPlayer) mP1 else mP2
                mPlayerWithFirstTurn = if (m1PSavedState.firstTurn) mP1 else mP2

                mBoard = Board.getBoard(mBoard.height, mBoard.width, savedData)
                updateBoardUi()

                displayBoard()
                updateScoreDisplay()
                mMatchInProgress = true
            } else {
                mMatchInProgress = false
            }
        }

        multiPlayer {
            initializeWaitingAnimations()
        }
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

        singlePlayer {
            if (mMatchInProgress) {
                m1PSavedState.save(mBoard, mCurrentPlayer!!, mPlayerWithFirstTurn!!)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_reversi, container, false)

    @OnClick(R.id.board_panel_new_game)
    internal fun onStartNewMatchClicked() {
        singlePlayer {
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

        multiPlayer {
            if (!mGoogleApiClient.isConnected) {
                mQueuedAction = QueuedAction.NewMatch
                displaySignInPrompt()
            } else {
                val intent: Intent = Games.TurnBasedMultiplayer
                        .getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true)
                startActivityForResult(intent, RC_SELECT_PLAYERS)
            }
        }
    }

    @OnClick(R.id.board_panel_select_game)
    internal fun onSelectMatchClicked() {
        // Button is hidden in single player
        multiPlayer {
            if (!mGoogleApiClient.isConnected) {
                mQueuedAction = QueuedAction.SelectMatch
                displaySignInPrompt()
            } else {
                val intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient)
                startActivityForResult(intent, RC_VIEW_MATCHES)
            }
        }
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

    private fun updateBoardUi(animate: Boolean = false) {
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
        singlePlayer {
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
            m1PSavedState.clear()
            mMatchInProgress = false
        }

        multiPlayer {
            updateScore()
            if (mMatch!!.status == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
                when (mPlayer!!.result.result) {
                    ParticipantResult.MATCH_RESULT_WIN -> {
                        displayMessage(getString(R.string.winner_you))
                    }
                    ParticipantResult.MATCH_RESULT_TIE -> {
                        displayMessage(getString(R.string.winner_tie))
                    }
                    ParticipantResult.MATCH_RESULT_LOSS -> {
                        val msg = if (mPlayer === mLightPlayer) R.string.winner_dark else R.string.winner_light
                        displayMessage(getString(msg))
                    }
                    else -> {
                        displayMessage(getString(R.string.match_complete))
                    }
                }

                if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    // Call finishMatch() to close out match for player
                    Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch!!.matchId)
                            .setResultCallback {
                                updateMatchResult -> processResultFinishMatch(updateMatchResult)
                            }
                }
            } else { // Match is not yet finished
                val winnerResult: ParticipantResult
                val loserResult: ParticipantResult
                if (mLightScore != mDarkScore) {
                    if (mLightScore > mDarkScore) {
                        winnerResult = ParticipantResult(
                                mLightPlayer!!.participantId,
                                ParticipantResult.MATCH_RESULT_WIN,
                                ParticipantResult.PLACING_UNINITIALIZED
                        )
                        loserResult = ParticipantResult(
                                mDarkPlayer!!.participantId,
                                ParticipantResult.MATCH_RESULT_LOSS,
                                ParticipantResult.PLACING_UNINITIALIZED
                        )
                        val msg = if (mPlayer === mLightPlayer) R.string.winner_you else R.string.winner_light
                        displayMessage(getString(msg))
                    } else {
                        winnerResult = ParticipantResult(
                                mDarkPlayer!!.participantId,
                                ParticipantResult.MATCH_RESULT_WIN,
                                ParticipantResult.PLACING_UNINITIALIZED
                        )
                        loserResult = ParticipantResult(
                                mLightPlayer!!.participantId,
                                ParticipantResult.MATCH_RESULT_LOSS,
                                ParticipantResult.PLACING_UNINITIALIZED
                        )
                        val msg = if (mPlayer === mDarkPlayer) R.string.winner_you else R.string.winner_dark
                        displayMessage(getString(msg))
                    }
                } else {
                    winnerResult = ParticipantResult(
                            mDarkPlayer!!.participantId,
                            ParticipantResult.MATCH_RESULT_TIE,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    loserResult = ParticipantResult(
                            mLightPlayer!!.participantId,
                            ParticipantResult.MATCH_RESULT_TIE,
                            ParticipantResult.PLACING_UNINITIALIZED
                    )
                    displayMessage(getString(R.string.winner_tie))
                }

                // Call finishMatch() with result parameters
                Games.TurnBasedMultiplayer.finishMatch(
                        mGoogleApiClient, mMatch!!.matchId, mMatchData,
                        winnerResult, loserResult
                )
                        .setResultCallback { updateMatchResult ->
                            processResultFinishMatch(updateMatchResult)
                        }
            }
        }
    }

    fun displayBoard() {
        mBoardPanelView.visibility = View.GONE
        mMatchGridView.visibility = View.VISIBLE
        updateBoardUi()
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

        singlePlayer {
            inflater.inflate(R.menu.single_player, menu)
        }

        multiPlayer {
            inflater.inflate(R.menu.multi_player, menu)
        }
    }

    //endregion


    //region Multi Player fragment

    // Create the Google API Client with access to Plus and Games
    internal fun buildGoogleApiClient(): GoogleApiClient {
        return GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build()
    }

    private fun connectGoogleApiClient() {
        multiPlayer {
            // Check if Google Play Services are available
            val result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity)
            if (result != ConnectionResult.SUCCESS) {
                autoConnectPreference = false
                showErrorDialog(result)
            } else {
                autoConnectPreference = true
                showSpinner()
                mGoogleApiClient.connect()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (mSignInOnStart) {
            connectGoogleApiClient()
        }
    }

    override fun onStop() {
        multiPlayer {
            mQueuedAction = null
            if (mGoogleApiClient.isConnected) {
                registerMatchUpdateListener(false)
                mGoogleApiClient.disconnect()
            }
        }

        super.onStop()
    }

    override fun onConnected(bundle: Bundle?) {
        Toast.makeText(context, "Connected to GPGS", Toast.LENGTH_SHORT).show()

        dismissSpinner()
        mIsSignedIn = true

        if (mSignOutOnConnect) {
            signOutFromGooglePlay()
            return
        }

        if (mQueuedAction != null) {
            when (mQueuedAction) {
                QueuedAction.NewMatch -> {
                    mQueuedAction = null
                    onStartNewMatchClicked()
                    return
                }
                QueuedAction.SelectMatch -> {
                    mQueuedAction = null
                    onSelectMatchClicked()
                    return
                }
                QueuedAction.ForfeitMatch -> {
                    mQueuedAction = null
                    forfeitMatchSelected()
                    return
                }
                QueuedAction.ShowAchievements -> {
                    mQueuedAction = null
                    showAchievements()
                    return
                }
            }
        }

        registerMatchUpdateListener(true)

        if (mMatch != null) {
            if (mMatch!!.data == null) {
                startMatch(mMatch!!)
            } else {
                updateMatch(mMatch!!)
            }
        }
    }

    override fun onConnectionSuspended(i: Int) {
        connectGoogleApiClient()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        dismissSpinner()

        if (mResolvingError) {
            return  // Already attempting to resolve an error
        }

        if (result.hasResolution()) {
            try {
                mResolvingError = true
                result.startResolutionForResult(activity, RC_RESOLVE_ERROR)
            } catch (e: IntentSender.SendIntentException) {
                connectGoogleApiClient()
            }

        } else { // Unresolvable error
            showErrorDialog(result.errorCode)
            mResolvingError = true
        }
    }

    private fun registerMatchUpdateListener(shouldRegister: Boolean) {
        Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(mGoogleApiClient)
        if (shouldRegister) {
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this)
        }
    }

    private fun displaySignInPrompt() {
        val dialog = AlertDialog.Builder(context)
                .setTitle(getString(R.string.dialog_sign_in_title))
                .setMessage(getString(R.string.dialog_sign_in_message))
                .setPositiveButton(getString(R.string.dialog_sign_in_confirm), onSignInConfirm())
                .setNegativeButton(getString(R.string.dialog_sign_in_cancel), { dialog, which -> })
                .setOnCancelListener { mQueuedAction = null }
                .create()
        showDialog(dialog)
    }

    fun onSignInConfirm() = DialogInterface.OnClickListener { dialog, which -> connectGoogleApiClient() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
        // Resolving error with Play Games
            RC_RESOLVE_ERROR -> handleError(resultCode, data)

        // Returned from the 'Select Match' dialog
            RC_VIEW_MATCHES -> handleSelectMatchResult(resultCode, data)

        // Returned from 'Select players to Invite' dialog
            RC_SELECT_PLAYERS -> handleSelectPlayersResult(resultCode, data)

        // Returned from achievements screen
            RC_SHOW_ACHIEVEMENTS -> handleShowAchievementsResult(resultCode, data)

        // Returned from settings screen
            RC_SETTINGS -> handleSettingsResult(resultCode, data)
        }
    }

    private fun handleError(resultCode: Int, data: Intent?) {
        mResolvingError = false
        if (resultCode == Activity.RESULT_OK) {
            if (!mGoogleApiClient.isConnecting && !mGoogleApiClient.isConnected) {
                connectGoogleApiClient()
            }
        }
    }

    private fun handleSelectMatchResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val match = data!!.getParcelableExtra<TurnBasedMatch>(Multiplayer.EXTRA_TURN_BASED_MATCH)
            if (match != null) {
                if (match.data == null) {
                    startMatch(match)
                } else {
                    updateMatch(match)
                }
            }
        } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            mIsSignedIn = false
            signOutFromGooglePlay()
        } else {
            showErrorDialog(resultCode)
        }
    }

    private fun handleSelectPlayersResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS)

            val minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0)
            val maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0)

            val autoMatchCriteria =
                    if (minAutoMatchPlayers > 0) {
                        RoomConfig.createAutoMatchCriteria(
                                minAutoMatchPlayers, maxAutoMatchPlayers, 0
                        )
                    } else {
                        mAchievementManager.unlock(Achievements.PLAY_WITH_FRIEND)
                        null
                    }

            val matchConfig = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria)
                    .build()

            showSpinner()
            Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, matchConfig)
                    .setResultCallback { processResult(it) }
        } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            mIsSignedIn = false
            signOutFromGooglePlay()
        } else {
            showErrorDialog(resultCode)
        }
    }

    private fun handleShowAchievementsResult(resultCode: Int, data: Intent?) {
        if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            // User signed out
            mIsSignedIn = false
            signOutFromGooglePlay()
        }
    }

    private fun handleSettingsResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            SettingsActivity.RESULT_SIGN_IN -> connectGoogleApiClient()
            SettingsActivity.RESULT_SIGN_OUT -> mSignOutOnConnect = true
        }
    }

    private fun signOutFromGooglePlay() {
        Toast.makeText(activity, R.string.sign_out_confirmation, Toast.LENGTH_SHORT).show()

        mSignOutOnConnect = false
        autoConnectPreference = false
        if (mGoogleApiClient.isConnected && mIsSignedIn) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient)
            Games.signOut(mGoogleApiClient)
        }
        mIsSignedIn = false
        mGoogleApiClient.disconnect()

        activity.setResult(SettingsActivity.RESULT_SIGN_OUT)
        activity.finish()
    }

    private fun processResult(result: TurnBasedMultiplayer.InitiateMatchResult) {
        val match = result.match
        mMatchData = null

        if (checkStatusCode(result.status.statusCode)) {
            if (match.data == null) {
                startMatch(match)
            } else {
                updateMatch(match)
            }
        }
    }

    private fun startMatch(match: TurnBasedMatch) {
        mMatch = match
        mMatchData = null
        mBoard.reset()
        saveMatchData()
        displayBoard()
        updateScore()

        val participantId = mMatch!!.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient))
        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.matchId,
                mMatchData, participantId).setResultCallback { result -> processResult(result) }
    }

    private fun processResult(result: TurnBasedMultiplayer.UpdateMatchResult) {
        mMatch = result.match
        dismissSpinner()

        if (checkStatusCode(result.status.statusCode)) {
            updateMatch(mMatch!!)
        }

        mUpdatingMatch = false
    }

    private fun updateMatch(match: TurnBasedMatch) {
        mUpdatingMatch = true
        mMatch = match
        mPlayer = currentPlayer
        mOpponent = opponent
        mLightPlayer = lightPlayer
        mDarkPlayer = darkPlayer
        mMatchData = match.data

        //        Log.d(TAG, "Match ID: " + mMatch.getMatchId());
        //        Log.d(TAG, bytesToString(mMatchData));
        //
        //        Log.d(TAG, "Match Status: " + mMatch.getStatus());
        //        Log.d(TAG, "Turn Status: " + mMatch.getTurnStatus());

        // Grab the appropriate segment from mMatchData based on player's color
        var startIndex = if (currentPlayer === lightPlayer) 0 else 100
        val playerData = Arrays.copyOfRange(mMatchData!!, startIndex, startIndex + 64)

        mBoard = Board.getBoard(mBoard.height, mBoard.width, playerData)
        displayBoard()
        dismissSpinner()

        // Commit opponent's moves to the deserialized Board object
        // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
        startIndex += 64
        while (mMatchData!![startIndex].toInt() != 0) {
            val s = mBoard.getBoardSpaceFromNum(mMatchData!![startIndex++].toInt())
            mQueuedMoves.add(s!!)
        }

        mUpdatingMatch = false
        if (!mQueuedMoves.isEmpty())
            processReceivedTurns()
        updateScore()

        // Check for inactive match states
        when (mMatch!!.status) {
            TurnBasedMatch.MATCH_STATUS_CANCELED -> {
                displayMessage(getString(R.string.match_canceled))
                return
            }
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                displayMessage(getString(R.string.match_expired))
                return
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> {
                displayMessage(getString(R.string.match_finding_partner))
                return
            }
            TurnBasedMatch.MATCH_STATUS_COMPLETE -> {
                endMatch()
                return
            }
        }

        // OK, it's active. Check on turn status.
        when (mMatch!!.turnStatus) {
            TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN -> {
                dismissMessage()
                //                autoplayIfEnabled();
                return
            }
            TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN -> {
                displayMessage(getString(R.string.match_opponent_turn))
                return
            }
            TurnBasedMatch.MATCH_TURN_STATUS_INVITED -> displayMessage(getString(R.string.match_invite_pending))
        }
    }

    private fun saveMatchData() {
        val playerBoard = mBoard.serialize()

        if (mMatchData == null) {
            mMatchData = ByteArray(256)
            System.arraycopy(playerBoard, 0, mMatchData!!, 0, playerBoard.size)
            System.arraycopy(playerBoard, 0, mMatchData!!, 100, playerBoard.size)
        } else {
            val startIndex = if (mPlayer === mLightPlayer) 0 else 100
            // Copy the serialized Board into the appropriate place in match data
            System.arraycopy(playerBoard, 0, mMatchData!!, startIndex, playerBoard.size)
            // Clear out the first 16 nodes following, which were the other player's previous moves
            for (clearIndex in startIndex + 64..startIndex + 64 + 16 - 1)
                mMatchData!![clearIndex] = 0
        }
    }

    private fun processReceivedTurns() {
        mUpdatingMatch = true
        mHandler!!.postDelayed({
            mBoard.commitPiece(mQueuedMoves.removeAt(0), opponentColor)
            saveMatchData()
            if (!mQueuedMoves.isEmpty())
                processReceivedTurns()
            else {
                mUpdatingMatch = false
                updateScore()
                //                    autoplayIfEnabled();
            }
        }, CPU_TURN_DELAY_MS)
    }

    override fun onTurnBasedMatchReceived(match: TurnBasedMatch) {
        if (mMatch != null && mMatch!!.matchId == match.matchId) {
            if (match.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
                // Turn still belongs to opponent, wait for another update
                return
            }
            updateMatch(match)
        }
    }

    override fun onTurnBasedMatchRemoved(matchId: String) {
        if (mMatch != null && mMatch!!.matchId == matchId) {
            Toast.makeText(activity, R.string.match_removed, Toast.LENGTH_SHORT).show()
            clearBoard()
        }
    }

    fun claim(s: BoardSpace) {
        if (mUpdatingMatch || !mQueuedMoves.isEmpty()) {
            return
        }

        if (mMatch!!.status != TurnBasedMatch.MATCH_STATUS_ACTIVE ||
                mMatch!!.turnStatus != TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            return
        }

        if (s.isOwned)
            return

        if (!mGoogleApiClient.isConnected) {
            displaySignInPrompt()
            return
        }

        val playerColor = currentPlayerColor
        mBoard.requestClaimSpace(s.y, s.x, playerColor)
                .subscribe({
                    mUpdatingMatch = true
                    showSpinner()
                    saveMatchData()

                    // Add selected piece to the end of mMatchData array
                    // 0 [Light's Board] 64 [Dark's Moves] 100 [Dark's Board] 164 [Light's Moves]
                    var nextIndex = if (mPlayer === mLightPlayer) 164 else 64
                    while (mMatchData!![nextIndex].toInt() != 0)
                        nextIndex++ // Increase index til we run into an unfilled index
                    mMatchData!![nextIndex] = mBoard.getSpaceNumber(s)

                    updateMatchState()
                }, {
                    Toast.makeText(activity, R.string.bad_move, Toast.LENGTH_SHORT).show()
                })
    }

    private fun updateMatchState() {
        if (mBoard.hasMove(opponentColor)) { // If opponent can make a move, it's his turn
            val pId = if (mOpponent == null) null else mOpponent!!.participantId
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch!!.matchId, mMatchData, pId)
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        } else if (mBoard.hasMove(currentPlayerColor)) { // Opponent has no move, keep turn
            val msg = getString(R.string.no_moves, mOpponent!!.displayName)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            Games.TurnBasedMultiplayer.takeTurn(
                    mGoogleApiClient, mMatch!!.matchId, mMatchData,
                    mPlayer!!.participantId
            )
                    .setResultCallback { updateMatchResult -> processResult(updateMatchResult) }
        } else { // No moves remaining, end of match
            endMatch()
            return
        }
        updateScore()
    }

    private val currentPlayer: Participant
        get() {
            val currentPlayerId = Games.Players.getCurrentPlayerId(mGoogleApiClient)
            if (mMatch!!.getParticipant(mMatch!!.getParticipantId(currentPlayerId)) === lightPlayer) {
                return lightPlayer!!
            } else {
                return darkPlayer!!
            }
        }

    private val opponent: Participant?
        get() = mMatch!!.descriptionParticipant

    private val currentPlayerColor: ReversiColor
        get() {
            if (mPlayer === mLightPlayer)
                return ReversiColor.LIGHT
            else
                return ReversiColor.DARK
        }

    private val opponentColor: ReversiColor
        get() {
            if (mOpponent === mLightPlayer)
                return ReversiColor.LIGHT
            else
                return ReversiColor.DARK
        }

    private val lightPlayer: Participant?
        get() {
            if (mMatch != null) {
                return mMatch!!.getParticipant(mMatch!!.creatorId)
            }
            return null
        }

    private val darkPlayer: Participant?
        get() {
            if (mMatch != null) {
                val participantIds = mMatch!!.participantIds
                val lightId = mMatch!!.creatorId

                val darkId: String? = participantIds.lastOrNull { it != lightId }

                if (darkId != null) {
                    return mMatch!!.getParticipant(darkId)
                }
            }
            return null
        }

    private fun clearBoard() {
        mMatch = null
        mMatchGridView.visibility = View.GONE
        mPlayerOneScoreTextView.text = ""
        mPlayerTwoScoreTextView.text = ""
        showWaitingIndicator(false, false)
        mBoardPanelView.visibility = View.VISIBLE
    }

    private fun updateScore() {
        mLightScore = mBoard.getNumSpacesForColor(ReversiColor.LIGHT)
        mDarkScore = mBoard.getNumSpacesForColor(ReversiColor.DARK)

        if (mMatch!!.status == TurnBasedMatch.MATCH_STATUS_COMPLETE && !mUpdatingMatch) {
            // Add remaining spaces to winning count as per Reversi rules
            if (mLightPlayer!!.result.result == ParticipantResult.MATCH_RESULT_WIN)
                mLightScore += mBoard.numberOfEmptySpaces
            else if (mDarkPlayer!!.result.result == ParticipantResult.MATCH_RESULT_WIN)
                mDarkScore += mBoard.numberOfEmptySpaces
        }

        mPlayerOneScoreTextView.text = mLightScore.toString()
        mPlayerTwoScoreTextView.text = mDarkScore.toString()

        // Update turn indicator
        when (mMatch!!.turnStatus) {
            TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN -> showWaitingIndicator(false, false)
            TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN -> showWaitingIndicator(false, true)
            TurnBasedMatch.MATCH_TURN_STATUS_INVITED -> showWaitingIndicator(false, false)
            TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE -> showWaitingIndicator(false, false)
        }
    }

    private fun checkStatusCode(statusCode: Int): Boolean {
        if (statusCode == GamesStatusCodes.STATUS_OK ||
                statusCode == GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED) {
            return true
        }

        clearBoard()
        dismissSpinner()

        when (statusCode) {
            GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_tester_untrusted)
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_already_rematched)
                )
            }
            GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_network_operation_failed)
                )
            }
            GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_reconnect_required)
                )
            }
            GamesStatusCodes.STATUS_INTERNAL_ERROR -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_internal_error)
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_inactive_match)
                )
            }
            GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_locally_modified)
                )
            }
            else -> {
                showAlertDialog(
                        getString(R.string.dialog_error_title),
                        getString(R.string.dialog_error_message_default)
                )
            }
        }

        return false
    }

    private fun displayMessage(matchMsg: String) {
        mMatchMessageTextView.text = matchMsg
        mMatchMessageView.visibility = View.VISIBLE

        // Start animations for side icons
        if (mLeftFadeOut != null && mRightFadeOut != null
                && !mLeftFadeOut!!.hasStarted() && !mRightFadeOut!!.hasStarted()) {
            mMatchMessageIcon1.startAnimation(mLeftFadeOut)
            mMatchMessageIcon2.startAnimation(mRightFadeOut)
        }
    }

    private fun dismissMessage() {
        mMatchMessageView.visibility = View.INVISIBLE
        mMatchMessageTextView.text = ""
        mLeftFadeOut!!.cancel()
        mRightFadeOut!!.cancel()
        mLeftFadeIn!!.cancel()
        mRightFadeIn!!.cancel()
    }

    private fun askForRematch() {
        AlertDialog.Builder(activity)
                .setTitle(getString(R.string.dialog_rematch_title))
                .setMessage(getString(R.string.dialog_rematch_message))
                .setPositiveButton(getString(R.string.dialog_rematch_confirm), onRematchConfirm())
                .setNegativeButton(getString(R.string.dialog_rematch_cancel), onRematchCancel())
                .setIcon(resources.getDrawable(R.drawable.ic_av_replay_blue))
                .create()
    }

    private fun onRematchConfirm() =
            DialogInterface.OnClickListener { dialog, which ->
                if (!mGoogleApiClient.isConnected) {
                    displaySignInPrompt()
                    return@OnClickListener
                }
                showSpinner()
                Games.TurnBasedMultiplayer.rematch(mGoogleApiClient, mMatch!!.matchId)
                        .setResultCallback { result -> processResult(result) }
                mMatch = null
            }

    private fun onRematchCancel() = DialogInterface.OnClickListener { dialog, which -> }

    private fun initializeWaitingAnimations() {
        mLeftFadeIn = AnimationUtils.loadAnimation(activity, R.anim.waitingmessage_fadein)
        mLeftFadeOut = AnimationUtils.loadAnimation(activity, R.anim.waitingmessage_fadeout)
        mRightFadeIn = AnimationUtils.loadAnimation(activity, R.anim.waitingmessage_fadein)
        mRightFadeOut = AnimationUtils.loadAnimation(activity, R.anim.waitingmessage_fadeout)

        mMatchMessageIcon1.setBackgroundResource(R.drawable.player_icon_p1)
        mMatchMessageIcon2.setBackgroundResource(R.drawable.player_icon_p2)

        mRightFadeOut!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                // Flip background resources & start animation
                mMatchMessageIcon2.setBackgroundResource(if (mMatchMessageIcon2Color)
                    R.drawable.player_icon_p1
                else
                    R.drawable.player_icon_p2)
                mMatchMessageIcon2Color = !mMatchMessageIcon2Color
                mMatchMessageIcon2.startAnimation(mRightFadeIn)
            }
        })

        mLeftFadeOut!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                // Flip background resources & start animation
                mMatchMessageIcon1.setBackgroundResource(if (mMatchMessageIcon1Color)
                    R.drawable.player_icon_p1
                else
                    R.drawable.player_icon_p2)
                mMatchMessageIcon1Color = !mMatchMessageIcon1Color
                mMatchMessageIcon1.startAnimation(mLeftFadeIn)
            }
        })

        mLeftFadeIn!!.setAnimationListener(
                object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}

                    override fun onAnimationEnd(animation: Animation) {
                        mHandler!!.postDelayed({
                            mMatchMessageIcon1.startAnimation(mLeftFadeOut)
                            mMatchMessageIcon2.startAnimation(mRightFadeOut)
                        }, resources.getInteger(R.integer.waiting_message_fade_delay).toLong())
                    }
                })
    }

    private fun showSpinner() {
        mProgressBar.show()
    }

    private fun dismissSpinner() {
        mProgressBar.dismiss()
    }

    private fun showDialog(dialog: Dialog) {
        if (mDisplayedDialog != null && mDisplayedDialog!!.isShowing) {
            mDisplayedDialog!!.dismiss()
        }
        mDisplayedDialog = dialog
        mDisplayedDialog!!.show()
    }

    // Generic warning/info dialog
    private fun showAlertDialog(title: String, message: String) {
        showDialog(AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_error_confirm)) { dialog, id -> }
                .create())
    }

    /* Creates a dialog for an error message */
    private fun showErrorDialog(errorCode: Int) {
        val dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, activity, RC_RESOLVE_ERROR)
        if (dialog != null) {
            dialog.setOnDismissListener { mResolvingError = false }
            dialog.show()
        }
    }

    private fun settingsSelected() {
        val settings = Intent(activity, SettingsActivity::class.java)
        settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_MULTI_PLAYER)
        val isSignedIn = mGoogleApiClient.isConnected
        settings.putExtra(SettingsActivity.EXTRA_IS_SIGNED_IN, isSignedIn)
        val accountName = if (isSignedIn) {
            Plus.AccountApi.getAccountName(mGoogleApiClient)
        } else {
            "" // FIXME: Should just pass in `null` here
        }
        settings.putExtra(SettingsActivity.EXTRA_SIGNED_IN_ACCOUNT, accountName)
        startActivityForResult(settings, RC_SETTINGS)
    }

    private fun forfeitMatchSelected() {
        if (mMatch == null) {
            Toast.makeText(activity, R.string.no_match_selected, Toast.LENGTH_LONG).show()
            return
        }

        if (!mGoogleApiClient.isConnected) {
            mQueuedAction = QueuedAction.ForfeitMatch
            displaySignInPrompt()
            return
        }

        when (mMatch!!.status) {
            TurnBasedMatch.MATCH_STATUS_COMPLETE,
            TurnBasedMatch.MATCH_STATUS_CANCELED,
            TurnBasedMatch.MATCH_STATUS_EXPIRED -> {
                Toast.makeText(activity, R.string.match_inactive, Toast.LENGTH_SHORT).show()
            }
            TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING -> showLeaveMatchDialog()
            TurnBasedMatch.MATCH_STATUS_ACTIVE -> {
                if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                    if (mOpponent == null) {
                        showLeaveMatchDialog()
                    } else {
                        if (mOpponent!!.status == Participant.STATUS_JOINED) {
                            showForfeitMatchDialog()
                        } else {
                            showCancelMatchDialog()
                        }
                    }
                } else {
                    showForfeitMatchForbiddenAlert()
                }
            }
        }

    }

    private fun showCancelMatchDialog() {
        showDialog(AlertDialog.Builder(activity)
                .setTitle(getString(R.string.dialog_cancel_match_title))
                .setMessage(getString(R.string.dialog_cancel_match_message))
                .setPositiveButton(getString(R.string.dialog_cancel_match_confirm), onCancelMatchConfirm())
                .setNegativeButton(getString(R.string.dialog_cancel_match_cancel), { dialog, which -> })
                .setCancelable(true)
                .create())
    }

    private fun onCancelMatchConfirm() = DialogInterface.OnClickListener { dialogInterface, which ->
        if (!mGoogleApiClient.isConnected) {
            displaySignInPrompt()
        } else {
            Games.TurnBasedMultiplayer.cancelMatch(mGoogleApiClient, mMatch!!.matchId)
                    .setResultCallback { cancelMatchResult -> processResult(cancelMatchResult) }
        }
    }

    private fun showForfeitMatchDialog() {
        showDialog(AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_forfeit_match_title)
                .setMessage(R.string.dialog_forfeit_match_message)
                .setPositiveButton(R.string.dialog_forfeit_match_confirm, onForfeitMatchConfirm())
                .setNegativeButton(R.string.dialog_forfeit_match_cancel, { dialog, which -> })
                .setCancelable(true)
                .create())
    }

    private fun onForfeitMatchConfirm() = DialogInterface.OnClickListener { dialogInterface, which ->
        if (!mGoogleApiClient.isConnected) {
            displaySignInPrompt()
            return@OnClickListener
        }

        val winnerResult = ParticipantResult(mOpponent!!.participantId,
                ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED)
        val loserResult = ParticipantResult(mPlayer!!.participantId,
                ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED)
        // Give win to other player
        Games.TurnBasedMultiplayer.finishMatch(
                mGoogleApiClient, mMatch!!.matchId, mMatchData,
                winnerResult, loserResult
        )
                .setResultCallback { result ->
                    if (result.status.isSuccess) {
                        Toast.makeText(activity, getString(R.string.forfeit_success), Toast.LENGTH_LONG).show()
                        updateMatch(result.match)
                    } else {
                        Toast.makeText(activity, getString(R.string.forfeit_fail), Toast.LENGTH_LONG).show()
                    }
                }
    }

    private fun showForfeitMatchForbiddenAlert() {
        showDialog(AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_forfeit_match_forbidden_title)
                .setMessage(R.string.dialog_forfeit_match_forbidden_message)
                .setPositiveButton(R.string.dialog_forfeit_match_forbidden_confirm) { dialog, which -> }
                .setCancelable(true)
                .create())
    }

    private fun showLeaveMatchDialog() {
        showDialog(AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_leave_match_title)
                .setMessage(R.string.dialog_leave_match_message)
                .setPositiveButton(R.string.dialog_leave_match_confirm, onLeaveMatchConfirm())
                .setNegativeButton(R.string.dialog_leave_match_cancel, { dialog, which -> })
                .setCancelable(true)
                .create())
    }

    private fun onLeaveMatchConfirm() = DialogInterface.OnClickListener { dialog, which ->
        if (!mGoogleApiClient.isConnected) {
            displaySignInPrompt()
            return@OnClickListener
        }

        if (mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            Games.TurnBasedMultiplayer.leaveMatchDuringTurn(mGoogleApiClient, mMatch!!.matchId, null)
                    .setResultCallback { result -> processResultLeaveMatch(result) }
        } else {
            Games.TurnBasedMultiplayer.leaveMatch(mGoogleApiClient, mMatch!!.matchId)
                    .setResultCallback { result -> processResultLeaveMatch(result) }
        }
    }

    private fun processResultFinishMatch(result: TurnBasedMultiplayer.UpdateMatchResult) {
        mUpdatingMatch = false
        dismissSpinner()
        if (checkStatusCode(result.status.statusCode)) {
            mMatch = result.match
            mPlayer = currentPlayer
            // Update achievements
            if (mPlayer!!.result.result == ParticipantResult.MATCH_RESULT_WIN) {
                mAchievementManager.unlock(Achievements.FIRST_WIN)
                if (playerScore == MAXIMUM_SCORE) {
                    mAchievementManager.unlock(Achievements.PERFECT_WIN)
                }
            } else if (mPlayer!!.result.result == ParticipantResult.MATCH_RESULT_TIE) {
                mAchievementManager.unlock(Achievements.TIE_GAME)
            }
            mAchievementManager.increment(Achievements.TEN_MATCHES, 1)
            mAchievementManager.increment(Achievements.HUNDRED_MATCHES, 1)

            if (mMatch!!.canRematch()) {
                askForRematch()
            }
        }
    }

    private fun processResultLeaveMatch(result: TurnBasedMultiplayer.LeaveMatchResult) {
        if (result.status.isSuccess) {
            Toast.makeText(activity, R.string.match_canceled_toast, Toast.LENGTH_SHORT).show()
            clearBoard()
        } else {
            Toast.makeText(activity, getString(R.string.cancel_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun processResult(result: TurnBasedMultiplayer.CancelMatchResult) {
        if (result.status.isSuccess) {
            Toast.makeText(activity, R.string.match_canceled_toast, Toast.LENGTH_SHORT).show()
            clearBoard()
        } else {
            Toast.makeText(activity, getString(R.string.cancel_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private val playerScore: Int
        get() = if (mPlayer === mLightPlayer) mLightScore else mDarkScore

    private fun showAchievements() {
        if (mGoogleApiClient.isConnected) {
            val intent = Games.Achievements.getAchievementsIntent(mGoogleApiClient)
            startActivityForResult(intent, RC_SHOW_ACHIEVEMENTS)
        } else {
            mQueuedAction = QueuedAction.ShowAchievements
            displaySignInPrompt()
        }
    }

    private var autoConnectPreference: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PREF_AUTO_SIGN_IN, false)
        set(b) {
            mSignInOnStart = b
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            prefs.edit().putBoolean(PREF_AUTO_SIGN_IN, b).apply()
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_match -> {
                onStartNewMatchClicked()
                return true
            }
            R.id.action_settings -> {
                val settings = Intent(activity, SettingsActivity::class.java)
                settings.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER)
                startActivity(settings)
                return true
            }
            R.id.action_how_to_play -> {
                val intent = Intent(activity, HowToPlayActivity::class.java)
                startActivity(intent)
                return true
            }
        }

        when (item.itemId) {
            R.id.action_create_match -> {
                onStartNewMatchClicked()
                return true
            }
            R.id.action_select_match -> {
                onSelectMatchClicked()
                return true
            }
            R.id.action_how_to_play -> {
                val intent = Intent(activity, HowToPlayActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_close_match -> {
                clearBoard()
                return true
            }
            R.id.action_forfeit_match -> {
                forfeitMatchSelected()
                return true
            }
            R.id.action_achievements -> {
                showAchievements()
                return true
            }
            R.id.action_settings -> {
                settingsSelected()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    // Added for testing full end-to-end multiplayer flow
    private fun autoplayIfEnabled() {
        if (!mUpdatingMatch && AUTOMATED_MULTIPLAYER
                && mMatch!!.status == TurnBasedMatch.MATCH_STATUS_ACTIVE
                && mMatch!!.turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            mHandler!!.postDelayed({
                // FIXME Why do we need to create new ReversiPlayers here?
                val color = if (mPlayer === mLightPlayer) ReversiColor.LIGHT else ReversiColor.DARK
                val p1 = ReversiPlayer(color, "")
                val p2 = ReversiPlayer(color.opposite(), "")
                claim(ComputerAI.getBestMove_d3(mBoard, p1, p2))
            }, 500)
        }
    }

    // Used for converting Board to debugging text
    private fun bytesToString(`in`: ByteArray?): String {
        if (`in` == null)
            return ""

        val buf = StringBuilder()

        buf.append("\n")
        for (i in 0..63) {
            buf.append(`in`[i].toString())
        }
        buf.append("\n")
        for (i in 64..99) {
            buf.append(`in`[i].toString()).append(" ")
        }
        buf.append("\n")
        for (i in 100..163) {
            buf.append(`in`[i].toString())
        }
        buf.append("\n")
        for (i in 164..199) {
            buf.append(`in`[i].toString()).append(" ")
        }

        return buf.toString()
    }

    //endregion
}
