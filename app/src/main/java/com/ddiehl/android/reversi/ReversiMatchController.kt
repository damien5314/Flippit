package com.ddiehl.android.reversi

import com.google.android.gms.games.multiplayer.Participant
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch

class ReversiMatchController {

    private var mMatch: TurnBasedMatch? = null
    private var mPlayer: Participant? = null
    private var mOpponent: Participant? = null
    private var mLightPlayer: Participant? = null
    private var mDarkPlayer: Participant? = null
    private var mMatchData: ByteArray? = null
    private var mLightScore: Int = 0
    private var mDarkScore: Int = 0

}
