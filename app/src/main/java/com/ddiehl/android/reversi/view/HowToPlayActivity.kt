package com.ddiehl.android.reversi.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.ddiehl.android.reversi.R

/**
 * Created from tutorial @ http://architects.dzone.com/articles/android-tutorial-using
 */
class HowToPlayActivity : ActionBarActivity() {

    private var mViewPager: ViewPager? = null
    private var mMenuPrevious: MenuItem? = null
    private var mMenuNext: MenuItem? = null

    private val FRAGMENT_LAYOUT_ID = intArrayOf(R.layout.activity_howtoplay_p1, R.layout.activity_howtoplay_p2, R.layout.activity_howtoplay_p3, R.layout.activity_howtoplay_p4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewPager = ViewPager(this)
        mViewPager!!.id = R.id.view_pager
        setContentView(mViewPager)

        val fm = supportFragmentManager
        mViewPager!!.adapter = object : FragmentStatePagerAdapter(fm) {
            override fun getItem(position: Int): Fragment {
                return HowToPlayFragment.newInstance(FRAGMENT_LAYOUT_ID[position])
            }

            override fun getCount(): Int {
                return FRAGMENT_LAYOUT_ID.size
            }
        }

        mViewPager!!.setOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(i: Int) {
                setMenuItemState(i)
            }
        })
    }

    private fun setMenuItemState(page: Int) {
        if (page == 0) {
            mMenuPrevious!!.isEnabled = false
        } else {
            mMenuPrevious!!.isEnabled = true
        }

        if (page == FRAGMENT_LAYOUT_ID.size - 1) {
            mMenuNext!!.isEnabled = false
        } else {
            mMenuNext!!.isEnabled = true
        }
    }

    class HowToPlayFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val layoutId = arguments.getInt(ARG_LAYOUT_ID)
            return inflater!!.inflate(layoutId, container, false)
        }

        companion object {
            private val ARG_LAYOUT_ID = "id"

            fun newInstance(id: Int): Fragment {
                val args = Bundle()
                args.putInt(ARG_LAYOUT_ID, id)
                val frag = HowToPlayFragment()
                frag.arguments = args
                return frag
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.how_to_play, menu)
        mMenuPrevious = menu.getItem(0)
        mMenuNext = menu.getItem(1)
        setMenuItemState(0)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_previous -> mViewPager!!.currentItem = mViewPager!!.currentItem - 1
            R.id.action_next -> mViewPager!!.currentItem = mViewPager!!.currentItem + 1
        }
        return true
    }

    companion object {
        private val TAG = HowToPlayActivity::class.java.simpleName
    }
}