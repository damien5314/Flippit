package com.ddiehl.android.reversi.game

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import butterknife.bindView
import com.ddiehl.android.reversi.R

class SinglePlayerMatchActivity : AppCompatActivity(), SpinnerView {

    internal val mToolbar by bindView<Toolbar>(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.match_activity)

        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val fm = supportFragmentManager
        var fragment: Fragment? = fm.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            fragment = MatchFragment.newInstance(false)
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
        }
    }


    //region MatchView

    private val mProgressBar: ProgressDialog by lazy {
        ProgressDialog(this, R.style.ProgressDialog).apply {
            setCancelable(true)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }
    }

    override fun showSpinner() {
        mProgressBar.show()
    }

    override fun dismissSpinner() {
        mProgressBar.dismiss()
    }

    //endregion
}
