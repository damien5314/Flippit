package com.ddiehl.android.flippit.activities;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import com.ddiehl.android.flippit.R;


public class HowToPlayActivity extends Activity {
    private static final String TAG = HowToPlayActivity.class.getSimpleName();

    public static class HowToPlayFragment extends Fragment {
        public HowToPlayFragment() { }

        public static Fragment newInstance() {
            HowToPlayFragment frag = new HowToPlayFragment();
            Bundle args = new Bundle();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_howtoplay);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new HowToPlayFragment()).commit();
    }

}
