package com.ddiehl.android.reversi.multiplayer

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games

interface AchievementManager {

    fun unlock(achievement: Achievements)

    fun increment(achievement: Achievements, count: Int)

    companion object {
        fun get(client: GoogleApiClient): AchievementManager {
            return Impl(client)
        }
    }

    private class Impl(private val client: GoogleApiClient) : AchievementManager {

        override fun unlock(achievement: Achievements) {
            Games.Achievements.unlock(client, achievement.id)
        }

        override fun increment(achievement: Achievements, count: Int) {
            Games.Achievements.increment(client, achievement.id, count)
        }
    }
}
