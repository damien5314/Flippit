package com.ddiehl.android.flippit.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        final int[] FRAGMENT_LAYOUT_ID = new int[] {
                R.layout.activity_howtoplay_p1,
                R.layout.activity_howtoplay_p1,
                R.layout.activity_howtoplay_p1
        };

        for (int id : FRAGMENT_LAYOUT_ID) {
            flist.add(HowToPlayFragment.newInstance(id));
        }

        return flist;
    }

    public static class HowToPlayFragment extends Fragment {
        private final static String LAYOUT_ID = "id";

        public HowToPlayFragment() { }

        public static Fragment newInstance(int id) {
            HowToPlayFragment frag = new HowToPlayFragment();
            Bundle args = new Bundle();
            args.putInt(LAYOUT_ID, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(getArguments().getInt(LAYOUT_ID), container, false);
        }
    }

}
