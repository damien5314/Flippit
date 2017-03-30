package com.ddiehl.android.reversi.game

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import butterknife.bindView
import com.ddiehl.android.reversi.MenuTintUtils
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.howtoplay.HowToPlayActivity
import com.ddiehl.android.reversi.model.Board
import com.ddiehl.android.reversi.startActivity

abstract class BaseMatchActivity : AppCompatActivity() {

    protected val mToolbar by bindView<Toolbar>(R.id.toolbar)
    protected val mMatchFragment by bindView<MatchFragment>(R.id.match_view)

    protected val mBoard: Board = Board(8, 8)

    override fun onStop() {
        mMatchFragment.dismissMessage()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.game_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        MenuTintUtils.tintAllIcons(menu, Color.WHITE)

        menu.findItem(R.id.action_new_match).isVisible = true
        menu.findItem(R.id.action_how_to_play).isVisible = true
        menu.findItem(R.id.action_settings).isVisible = true

        if (this is SinglePlayerMatchActivity) {
            menu.findItem(R.id.action_select_match).isVisible = false
            menu.findItem(R.id.action_close_match).isVisible = false
            menu.findItem(R.id.action_forfeit_match).isVisible = false
            menu.findItem(R.id.action_achievements).isVisible = false
        }

        if (this is MultiPlayerMatchActivity) {
            menu.findItem(R.id.action_select_match).isVisible = true
            menu.findItem(R.id.action_close_match).isVisible = true
            menu.findItem(R.id.action_forfeit_match).isVisible = true
            menu.findItem(R.id.action_achievements).isVisible = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (!isFinishing) finish()
                return true
            }
            R.id.action_new_match -> {
                onStartNewMatchClicked()
                return true
            }
            R.id.action_how_to_play -> {
                startActivity<HowToPlayActivity>(this)
                return true
            }
            R.id.action_settings -> {
                onSettingsClicked()
                return true
            }
            R.id.action_select_match -> {
                onSelectMatchClicked()
                return true
            }
            R.id.action_close_match -> {
                onCloseMatchClicked()
                return true
            }
            R.id.action_forfeit_match -> {
                onForfeitMatchClicked()
                return true
            }
            R.id.action_achievements -> {
                onShowAchievementsClicked()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    abstract fun onStartNewMatchClicked()
    abstract fun onSelectMatchClicked()
    abstract fun onCloseMatchClicked()
    abstract fun onForfeitMatchClicked()
    abstract fun onShowAchievementsClicked()
    abstract fun onSettingsClicked()
}
