package com.ddiehl.android.reversi.howtoplay

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.how_to_play, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.getItem(0)!!.isEnabled = mPage != 0
        menu.getItem(1)!!.isEnabled = mPage != FRAGMENT_LAYOUT_ID.size - 1
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_previous -> mViewPager.currentItem = mViewPager.currentItem - 1
            R.id.action_next -> mViewPager.currentItem = mViewPager.currentItem + 1
        }
        return true
    }
}

class HowToPlayFragment : Fragment() {

    companion object {
        private val ARG_LAYOUT_ID = "id"

        fun newInstance(id: Int): Fragment {
            return HowToPlayFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_LAYOUT_ID, id)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater!!.inflate(arguments.getInt(ARG_LAYOUT_ID), container, false)
}
