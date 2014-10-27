package com.ddiehl.android.reversi.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.adapters.HowToPlayPageAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created from tutorial @ http://architects.dzone.com/articles/android-tutorial-using
 */
public class HowToPlayActivity extends FragmentActivity {
    private static final String TAG = HowToPlayActivity.class.getSimpleName();
    ViewPager pager;
    HowToPlayPageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_howtoplay);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        List<Fragment> fragments = getFragments();
        pageAdapter = new HowToPlayPageAdapter(getSupportFragmentManager(), fragments);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(pageAdapter);
    }

    private List<Fragment> getFragments() {
        List<Fragment> flist = new ArrayList<Fragment>();
        final int[] FRAGMENT_LAYOUT_ID = new int[] {
                R.layout.activity_howtoplay_p1,
                R.layout.activity_howtoplay_p2,
                R.layout.activity_howtoplay_p3,
                R.layout.activity_howtoplay_p4
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.how_to_play, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.previous:
                if (pager.getCurrentItem() > 0)
                    pager.setCurrentItem(pager.getCurrentItem()-1);
                return true;
            case R.id.next:
                if (pager.getCurrentItem() < pager.getChildCount())
                    pager.setCurrentItem(pager.getCurrentItem()+1);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
