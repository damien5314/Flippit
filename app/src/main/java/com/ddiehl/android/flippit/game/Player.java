package com.ddiehl.android.flippit.game;

public class Player {
	private ReversiColor color;
	private String name;
	private int score;

	private Player() { }

	public Player(ReversiColor c, String n) {
		color = c;
		name = n;
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
}
