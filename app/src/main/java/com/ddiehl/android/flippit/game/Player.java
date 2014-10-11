package com.ddiehl.android.flippit.game;

public class Player {
	private ReversiColor color;
	private int turn = 0;

	private Player() { }

	public Player(ReversiColor c) {
		color = c;
	}

	public ReversiColor getColor() {
		return color;
	}

}
