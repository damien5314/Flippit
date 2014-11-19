package com.ddiehl.android.reversi.activities;

import android.app.Activity;

import com.ddiehl.android.reversi.game.BoardSpace;


public abstract class MatchActivity extends Activity {

	public abstract void claim(BoardSpace s);

}
