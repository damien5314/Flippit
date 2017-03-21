package com.ddiehl.android.reversi.settings

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import butterknife.bindView
import com.ddiehl.android.reversi.R

class SettingsActivity : AppCompatActivity() {

    companion object {
        val EXTRA_SETTINGS_MODE = "settings_mode"
        val EXTRA_IS_SIGNED_IN = "is_signed_in"
        val EXTRA_SIGNED_IN_ACCOUNT = "signed_in_account"

        val SETTINGS_MODE_SINGLE_PLAYER = 101
        val SETTINGS_MODE_MULTI_PLAYER = 102
        val RESULT_SIGN_IN = 201
        val RESULT_SIGN_OUT = 202
    }

    val mToolbar by bindView<Toolbar>(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setTitle(R.string.settings)
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.gray90))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (!isFinishing) finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
