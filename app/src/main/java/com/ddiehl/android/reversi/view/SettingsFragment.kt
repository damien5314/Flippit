package com.ddiehl.android.reversi.view

import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.MultiSelectListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceGroup
import android.preference.PreferenceManager

import com.ddiehl.android.reversi.R

/**
 * Adapted to PreferenceFragment from http://stackoverflow.com/a/4325239/3238938
 */
class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = activity

        addPreferencesFromResource(R.xml.preferences)
        PreferenceManager.setDefaultValues(this.activity, R.xml.preferences, false)

        val extras = arguments

        // Show/Hide different options based on current game mode
        when (arguments.getInt(SettingsActivity.EXTRA_SETTINGS_MODE)) {
            SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER -> {
                preferenceScreen.removePreference(findPreference(SettingsActivity.PREF_PLAY_SERVICES_SIGN_IN))
                initSummary(preferenceScreen)
            }
            SettingsActivity.SETTINGS_MODE_MULTI_PLAYER -> {
                preferenceScreen.removePreference(findPreference(SettingsActivity.PREF_PLAYER_NAME))
                preferenceScreen.removePreference(findPreference(SettingsActivity.PREF_AI_DIFFICULTY))
                initializeSignInPreference(findPreference(SettingsActivity.PREF_PLAY_SERVICES_SIGN_IN),
                        extras.getBoolean(SettingsActivity.EXTRA_IS_SIGNED_IN),
                        extras.getString(SettingsActivity.EXTRA_SIGNED_IN_ACCOUNT))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun initSummary(p: Preference) {
        if (p is PreferenceGroup) {
            for (i in 0..p.preferenceCount - 1) {
                initSummary(p.getPreference(i))
            }
        } else {
            updatePrefSummary(p)
        }
    }

    private fun updatePrefSummary(p: Preference) {
        if (p is ListPreference) {
            p.setSummary(p.entry)
        }
        if (p is EditTextPreference) {
            if (p.getTitle().toString().toLowerCase().contains("password")) {
                p.setSummary("******")
            } else {
                p.setSummary(p.text)
            }
        }
        if (p is MultiSelectListPreference) {
            val editTextPref = p as EditTextPreference
            p.setSummary(editTextPref.text)
        }
    }

    private fun initializeSignInPreference(p: Preference, isSignedIn: Boolean, accountName: String) {
        if (isSignedIn) {
            p.title = getString(R.string.pref_play_services_sign_out)
            p.summary = accountName
            p.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showDialogForSignOut()
                true
            }
        } else {
            p.title = getString(R.string.pref_play_services_sign_in)
            p.summary = getString(R.string.pref_play_services_sign_in_summary)
            p.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showDialogForSignIn()
                true
            }
        }
    }

    private fun showDialogForSignIn() {
        AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.settings_dialog_sign_in_title))
                .setMessage(getString(R.string.settings_dialog_sign_in_message))
                .setPositiveButton(getString(R.string.settings_dialog_sign_in_confirm)) { dialog, which ->
                    (mContext as Activity).setResult(SettingsActivity.RESULT_SIGN_IN)
                    (mContext as Activity).finish()
                }
                .setNegativeButton(getString(R.string.settings_dialog_sign_in_cancel)) { dialog, which -> }
                .setCancelable(true)
                .show()
    }

    private fun showDialogForSignOut() {
        AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.settings_dialog_sign_out_title))
                .setMessage(getString(R.string.settings_dialog_sign_out_message))
                .setPositiveButton(getString(R.string.settings_dialog_sign_out_confirm)) { dialog, which ->
                    (mContext as Activity).setResult(SettingsActivity.RESULT_SIGN_OUT)
                    (mContext as Activity).finish()
                }
                .setNegativeButton(getString(R.string.settings_dialog_sign_out_cancel)) { dialog, which -> }
                .setCancelable(true)
                .show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updatePrefSummary(findPreference(key))
    }

    companion object {

        fun newInstance(intent: Intent): Fragment {
            val frag = SettingsFragment()
            val args = intent.extras
            frag.arguments = args
            return frag
        }
    }
}
