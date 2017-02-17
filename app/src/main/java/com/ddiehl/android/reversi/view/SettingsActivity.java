package com.ddiehl.android.reversi.view;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String EXTRA_SETTINGS_MODE = "settings_mode";
    public static final String EXTRA_IS_SIGNED_IN = "is_signed_in";
    public static final String EXTRA_SIGNED_IN_ACCOUNT = "signed_in_account";

    public static final String PREF_PLAY_SERVICES_SIGN_IN = "pref_play_services_sign_in";
    public static final String PREF_PLAYER_NAME = "pref_player_name";
    public static final String PREF_AI_DIFFICULTY = "pref_ai_difficulty";

    public static final int SETTINGS_MODE_SINGLE_PLAYER = 101;
    public static final int SETTINGS_MODE_MULTI_PLAYER = 102;
    public static final int RESULT_SIGN_IN = 201;
    public static final int RESULT_SIGN_OUT = 202;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, SettingsFragment.newInstance(getIntent())).commit();
    }
}
