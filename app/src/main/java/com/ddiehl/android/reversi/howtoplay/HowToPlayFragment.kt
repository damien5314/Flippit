package com.ddiehl.android.reversi.howtoplay

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, state: Bundle?): View
            = inflater!!.inflate(arguments.getInt(ARG_LAYOUT_ID), container, false)
}
