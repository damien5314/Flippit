package com.ddiehl.android.flippit.game;

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

	public ReversiColor getColor() {
		return color;
	}

	public String getName() {
		return name;
	}

	public void setName(String s) {
		name = s;
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
