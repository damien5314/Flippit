package com.ddiehl.android.reversi.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import com.ddiehl.android.reversi.R;

/**
 * Adapted to PreferenceFragment from http://stackoverflow.com/a/4325239/3238938
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;

    public SettingsFragment() { }

    public static Fragment newInstance(Intent intent) {
        SettingsFragment frag = new SettingsFragment();
        Bundle args = intent.getExtras();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(this.getActivity(), R.xml.preferences, false);

        Bundle extras = getArguments();

        // Show/Hide different options based on current game mode
        switch (getArguments().getInt(SettingsActivity.EXTRA_SETTINGS_MODE)) {
            case SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER:
                getPreferenceScreen().removePreference(findPreference(SettingsActivity.PREF_PLAY_SERVICES_SIGN_IN));
                initSummary(getPreferenceScreen());
                break;
            case SettingsActivity.SETTINGS_MODE_MULTI_PLAYER:
                getPreferenceScreen().removePreference(findPreference(SettingsActivity.PREF_PLAYER_NAME));
                getPreferenceScreen().removePreference(findPreference(SettingsActivity.PREF_AI_DIFFICULTY));
                initializeSignInPreference(findPreference(SettingsActivity.PREF_PLAY_SERVICES_SIGN_IN),
                        extras.getBoolean(SettingsActivity.EXTRA_IS_SIGNED_IN),
                        extras.getString(SettingsActivity.EXTRA_SIGNED_IN_ACCOUNT));
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().toLowerCase().contains("password"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }

    private void initializeSignInPreference(Preference p, boolean isSignedIn, String accountName) {
        if (isSignedIn) {
            p.setTitle(getString(R.string.pref_play_services_sign_out));
            p.setSummary(accountName);
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDialogForSignOut();
                    return true;
                }
            });
        } else {
            p.setTitle(getString(R.string.pref_play_services_sign_in));
            p.setSummary(getString(R.string.pref_play_services_sign_in_summary));
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDialogForSignIn();
                    return true;
                }
            });
        }
    }

    private void showDialogForSignIn() {
        new AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.settings_dialog_sign_in_title))
                .setMessage(getString(R.string.settings_dialog_sign_in_message))
                .setPositiveButton(getString(R.string.settings_dialog_sign_in_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Activity) mContext).setResult(SettingsActivity.RESULT_SIGN_IN);
                        ((Activity) mContext).finish();
                    }
                })
                .setNegativeButton(getString(R.string.settings_dialog_sign_in_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }
                })
                .setCancelable(true)
                .show();
    }

    private void showDialogForSignOut() {
        new AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.settings_dialog_sign_out_title))
                .setMessage(getString(R.string.settings_dialog_sign_out_message))
                .setPositiveButton(getString(R.string.settings_dialog_sign_out_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Activity) mContext).setResult(SettingsActivity.RESULT_SIGN_OUT);
                        ((Activity) mContext).finish();
                    }
                })
                .setNegativeButton(getString(R.string.settings_dialog_sign_out_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }
}
