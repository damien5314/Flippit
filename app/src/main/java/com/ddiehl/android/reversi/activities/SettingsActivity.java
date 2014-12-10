package com.ddiehl.android.reversi.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.ddiehl.android.reversi.R;

/**
 * Adapted to PreferenceFragment from http://stackoverflow.com/a/4325239/3238938
 */
public class SettingsActivity extends Activity {
	private static final String TAG = SettingsActivity.class.getSimpleName();
	public static final String EXTRA_SETTINGS_MODE = "settings_mode";
	public static final int SETTINGS_MODE_SINGLE_PLAYER = 101;
	public static final int SETTINGS_MODE_MULTI_PLAYER = 102;

	public static class SettingsFragment extends PreferenceFragment
			implements SharedPreferences.OnSharedPreferenceChangeListener {

		public SettingsFragment() { }

		public static Fragment newInstance(int mode) {
			SettingsFragment frag = new SettingsFragment();
			Bundle args = new Bundle();
			args.putInt(EXTRA_SETTINGS_MODE, mode);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
			PreferenceManager.setDefaultValues(this.getActivity(), R.xml.preferences, false);
			initSummary(getPreferenceScreen());

			PreferenceScreen prefs = ((PreferenceScreen) findPreference("pref_category"));
//			prefs.removeAll();
			// Show/Hide different options based on current game mode
			switch (getArguments().getInt(EXTRA_SETTINGS_MODE)) {
				case SETTINGS_MODE_SINGLE_PLAYER:
					prefs.removePreference(findPreference("pref_play_services_sign_in"));
					break;
				case SETTINGS_MODE_MULTI_PLAYER:
					prefs.removePreference(findPreference("pref_player_name"));
					prefs.removePreference(findPreference("pref_ai_difficulty"));
					break;
				default:
					Log.d(TAG, "Unrecognized mode: " + getArguments().getInt(EXTRA_SETTINGS_MODE));
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			// Set up a listener whenever a key changes
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			super.onPause();
			// Unregister the listener whenever a key changes
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
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

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			updatePrefSummary(findPreference(key));
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		int mode = 0;
		if (extras.containsKey(EXTRA_SETTINGS_MODE))
			mode = extras.getInt(EXTRA_SETTINGS_MODE);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content,
						SettingsFragment.newInstance(mode))
				.commit();
	}
}
