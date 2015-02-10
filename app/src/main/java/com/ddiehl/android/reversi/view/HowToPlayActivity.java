package com.ddiehl.android.reversi.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ddiehl.android.reversi.R;

/**
 * Created from tutorial @ http://architects.dzone.com/articles/android-tutorial-using
 */
public class HowToPlayActivity extends ActionBarActivity {
    private static final String TAG = HowToPlayActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private MenuItem mMenuPrevious, mMenuNext;

    private final int[] FRAGMENT_LAYOUT_ID = new int[] {
            R.layout.activity_howtoplay_p1,
            R.layout.activity_howtoplay_p2,
            R.layout.activity_howtoplay_p3,
            R.layout.activity_howtoplay_p4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.view_pager);
        setContentView(mViewPager);

        FragmentManager fm = getSupportFragmentManager();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fm) {
            @Override
            public Fragment getItem(int position) {
                return HowToPlayFragment.newInstance(FRAGMENT_LAYOUT_ID[position]);
            }

            @Override
            public int getCount() {
                return FRAGMENT_LAYOUT_ID.length;
            }
        });

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                setMenuItemState(i);
            }
        });
    }

    private void setMenuItemState(int page) {
        if (page == 0) {
            mMenuPrevious.setEnabled(false);
        } else {
            mMenuPrevious.setEnabled(true);
        }

        if (page == FRAGMENT_LAYOUT_ID.length - 1) {
            mMenuNext.setEnabled(false);
        } else {
            mMenuNext.setEnabled(true);
        }
    }

    public static class HowToPlayFragment extends Fragment {
        private final static String ARG_LAYOUT_ID = "id";

        public HowToPlayFragment() { }

        public static Fragment newInstance(int id) {
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_ID, id);
            HowToPlayFragment frag = new HowToPlayFragment();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int layoutId = getArguments().getInt(ARG_LAYOUT_ID);
            return inflater.inflate(layoutId, container, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.how_to_play, menu);
        mMenuPrevious = menu.getItem(0);
        mMenuNext = menu.getItem(1);
        setMenuItemState(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_previous:
                mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
                break;
            case R.id.action_next:
                mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1);
                break;
        }
        return true;
    }
}
