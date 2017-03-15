package com.ddiehl.android.reversi.game

import android.content.Intent
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
import com.ddiehl.android.reversi.settings.SettingsActivity

abstract class BaseMatchActivity : AppCompatActivity() {

    protected val mToolbar by bindView<Toolbar>(R.id.toolbar)
    protected val mMatchFragment by bindView<MatchFragment>(R.id.match_fragment)

    protected val mBoard: Board = Board(8, 8)

    override fun onStop() {
        mMatchFragment.dismissMessage()

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.multi_player, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        MenuTintUtils.tintAllIcons(menu, Color.WHITE)
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
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra(SettingsActivity.EXTRA_SETTINGS_MODE, SettingsActivity.SETTINGS_MODE_SINGLE_PLAYER)
                startActivity(intent)
                return true
            }
            R.id.action_how_to_play -> {
                val intent = Intent(this, HowToPlayActivity::class.java)
                startActivity(intent)
                return true
            }
        }

        when (item.itemId) {
            R.id.action_create_match -> {
                onStartNewMatchClicked()
                return true
            }
            R.id.action_select_match -> {
                onSelectMatchClicked()
                return true
            }
            R.id.action_how_to_play -> {
                val intent = Intent(this, HowToPlayActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_close_match -> {
                clearBoard()
                return true
            }
            R.id.action_forfeit_match -> {
                forfeitMatchSelected()
                return true
            }
            R.id.action_achievements -> {
                showAchievements()
                return true
            }
            R.id.action_settings -> {
                settingsSelected()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    abstract fun onStartNewMatchClicked()
    abstract fun onSelectMatchClicked()
    abstract fun clearBoard()
    abstract fun forfeitMatchSelected()
    abstract fun showAchievements()
    abstract fun settingsSelected()
}
