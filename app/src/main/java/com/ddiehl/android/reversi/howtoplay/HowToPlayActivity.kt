package com.ddiehl.android.reversi.howtoplay

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import butterknife.bindView
import com.ddiehl.android.reversi.R
import me.relex.circleindicator.CircleIndicator

class HowToPlayActivity : AppCompatActivity() {

    private val mToolbar by bindView<Toolbar>(R.id.toolbar)
    private val mViewPager by bindView<ViewPager>(R.id.view_pager)
    private val mViewPagerIndicator by bindView<CircleIndicator>(R.id.view_pager_indicator)
    private var mPage: Int = 0

    private val FRAGMENT_LAYOUT_ID = intArrayOf(
            R.layout.how_to_play_1,
            R.layout.how_to_play_2
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.how_to_play_activity)

        setSupportActionBar(mToolbar)

        mViewPager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return HowToPlayFragment.newInstance(FRAGMENT_LAYOUT_ID[position])
            }

            override fun getCount(): Int {
                return FRAGMENT_LAYOUT_ID.size
            }
        }

        mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mPage = position
                invalidateOptionsMenu()
            }
        })

        mViewPagerIndicator.setViewPager(mViewPager)
    }
}
