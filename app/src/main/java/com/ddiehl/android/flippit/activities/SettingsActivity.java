package com.ddiehl.android.flippit.activities;

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

import com.ddiehl.android.flippit.R;

/**
 * Adapted to PreferenceFragment from http://stackoverflow.com/a/4325239/3238938
 */
public class SettingsActivity extends Activity {

	public static class SettingsFragment extends PreferenceFragment
			implements SharedPreferences.OnSharedPreferenceChangeListener {
		public SettingsFragment() {

		}

		public static Fragment newInstance() {
			SettingsFragment frag = new SettingsFragment();
			Bundle args = new Bundle();
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
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();
	}
}
