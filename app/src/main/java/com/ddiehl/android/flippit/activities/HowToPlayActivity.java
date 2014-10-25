package com.ddiehl.android.flippit.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ddiehl.android.flippit.R;
import com.ddiehl.android.flippit.adapters.MyPageAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created from tutorial @ http://architects.dzone.com/articles/android-tutorial-using
 */
public class HowToPlayActivity extends FragmentActivity {
    private static final String TAG = HowToPlayActivity.class.getSimpleName();
    MyPageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_howtoplay);
        List<Fragment> fragments = getFragments();
        pageAdapter = new MyPageAdapter(getSupportFragmentManager(), fragments);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(pageAdapter);
//        getFragmentManager().beginTransaction()
//                .replace(android.R.id.content, new HowToPlayFragment()).commit();
    }

    private List<Fragment> getFragments() {
        List<Fragment> flist = new ArrayList<Fragment>();

        flist.add(HowToPlayFragment.newInstance("Fragment 1"));
        flist.add(HowToPlayFragment.newInstance("Fragment 2"));
        flist.add(HowToPlayFragment.newInstance("Fragment 3"));

        return flist;
    }

    public static class HowToPlayFragment extends Fragment {
        private final static String EXTRA_MESSAGE = "EXTRA_MESSAGE";

        public HowToPlayFragment() { }

        public static Fragment newInstance(String s) {
            HowToPlayFragment frag = new HowToPlayFragment();
            Bundle args = new Bundle();
            args.putString(EXTRA_MESSAGE, s);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            String message = getArguments().getString(EXTRA_MESSAGE);
            View v = inflater.inflate(R.layout.activity_howtoplay_fragment, container, false);
            TextView tv = (TextView) v.findViewById(R.id.textView);
            tv.setText(message);

            return v;
        }
    }

}
