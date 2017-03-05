package com.ddiehl.android.reversi.view

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.OnClick
import com.ddiehl.android.reversi.R

class LauncherFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_launcher, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view!!)
    }

    @OnClick(R.id.button_start_1p)
    fun onSinglePlayerClicked() {
        val intent = Intent(activity, SinglePlayerMatchActivity::class.java)
        startActivity(intent)
    }

    @OnClick(R.id.button_start_mp)
    fun onMultiPlayerClicked() {
        val intent = Intent(activity, MultiPlayerMatchActivity::class.java)
        startActivity(intent)
    }
}
