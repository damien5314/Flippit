package com.ddiehl.android.flippit.game;

import android.util.Log;

public class Player {
	private static final String TAG = Player.class.getSimpleName();
	private ReversiColor color;
	private String name;
	private int score;
	private boolean isCPU;

	private Player() { }

	public Player(ReversiColor c, String n) {
		color = c;
		name = n;
		isCPU = false;
	}

	public BoardSpace performMove(Board b) {
		if (!isCPU)
			return null;

		Move best = null;
		int bestVal = -1;
		for (int y = 0; y < b.height(); y++) {
			for (int x = 0; x < b.width(); x++) {
				if (!b.getSpaceAt(x, y).isOwned()) {
					int val = b.moveValue(x, y, getColor());
					if (val > bestVal) {
						bestVal = val;
						best = new Move();
						best.x = x; best.y = y;
					}
				}
			}
		}

		if (best != null) {
			Log.i(TAG, "Best move @(" + best.x + "," + best.y + ") has value of " + bestVal);
//			b.setPieceOn(best.x, best.y, getColor());
			return b.getSpaceAt(best.x, best.y);
		}

		return null;
	}

	private class Move {
		public int x, y;
	}

	public ReversiColor getColor() {
		return color;
	}

	public String getName() {
		return name;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int s) {
		score = s;
	}

	public boolean isCPU() {
		return isCPU;
	}

	public void isCPU(boolean b) {
		isCPU = b;
	}
}
