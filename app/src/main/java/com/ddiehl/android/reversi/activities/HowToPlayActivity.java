package com.ddiehl.android.reversi.activities;

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

    private ViewPager pager;
    private MenuItem previous, next;

    private final int[] FRAGMENT_LAYOUT_ID = new int[] {
            R.layout.activity_howtoplay_p1,
            R.layout.activity_howtoplay_p2,
            R.layout.activity_howtoplay_p3,
            R.layout.activity_howtoplay_p4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_howtoplay);
        List<Fragment> fragments = getFragments();
        HowToPlayPageAdapter pageAdapter = new HowToPlayPageAdapter(getSupportFragmentManager(), fragments);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(pageAdapter);
        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                if (i == 0)
                    previous.setEnabled(false);
                else
                    previous.setEnabled(true);

                if (i == FRAGMENT_LAYOUT_ID.length - 1)
                    next.setEnabled(false);
                else
                    next.setEnabled(true);
            }
        });
    }

    private List<Fragment> getFragments() {
        List<Fragment> fragmentList = new ArrayList<>();
        for (int id : FRAGMENT_LAYOUT_ID) {
            fragmentList.add(HowToPlayFragment.newInstance(id));
        }
        return fragmentList;
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
        previous = menu.getItem(0);
        previous.setEnabled(false);
        next = menu.getItem(1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_previous:
                pager.setCurrentItem(pager.getCurrentItem()-1);
                break;
            case R.id.action_next:
                pager.setCurrentItem(pager.getCurrentItem()+1);
                break;
        }
        return true;
    }
}
