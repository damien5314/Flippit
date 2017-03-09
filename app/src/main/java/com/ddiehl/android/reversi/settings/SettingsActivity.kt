package com.ddiehl.android.reversi.settings

import android.R
import android.app.Activity
import android.os.Bundle

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
                .replace(R.id.content, SettingsFragment.newInstance(intent)).commit()
    }

    companion object {
        val EXTRA_SETTINGS_MODE = "settings_mode"
        val EXTRA_IS_SIGNED_IN = "is_signed_in"
        val EXTRA_SIGNED_IN_ACCOUNT = "signed_in_account"

        val PREF_PLAY_SERVICES_SIGN_IN = "pref_play_services_sign_in"
        val PREF_PLAYER_NAME = "pref_player_name"
        val PREF_AI_DIFFICULTY = "pref_ai_difficulty"

        val SETTINGS_MODE_SINGLE_PLAYER = 101
        val SETTINGS_MODE_MULTI_PLAYER = 102
        val RESULT_SIGN_IN = 201
        val RESULT_SIGN_OUT = 202
    }
}
