package com.ddiehl.android.reversi.launcher

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.game.MultiPlayerMatchActivity
import com.ddiehl.android.reversi.game.SinglePlayerMatchActivity
import com.ddiehl.android.reversi.startActivity

class LauncherFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, state: Bundle?): View
            = inflater!!.inflate(R.layout.launcher_fragment, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity.findViewById(R.id.button_start_1p) as View)
                .setOnClickListener { onSinglePlayerClicked() }

        (activity.findViewById(R.id.button_start_mp) as View)
                .setOnClickListener { onMultiPlayerClicked() }
    }

    override fun onDestroyView() {
        ButterKnife.reset(this)
        super.onDestroyView()
    }

    private fun onSinglePlayerClicked() {
        startActivity<SinglePlayerMatchActivity>(context)
    }

    private fun onMultiPlayerClicked() {
        startActivity<MultiPlayerMatchActivity>(context)
    }
}
