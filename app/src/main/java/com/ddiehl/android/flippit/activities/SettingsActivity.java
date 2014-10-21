package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.ddiehl.android.flippit.R;


public class SettingsActivity extends Activity {
	private static final String TAG = SettingsActivity.class.getSimpleName();
//	public static final String CONFIG_NAME = "pref";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();
	}

	public static class SettingsFragment extends PreferenceFragment {
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
		}
	}
}
