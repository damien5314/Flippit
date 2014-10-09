package com.ddiehl.android.flippit.game;

public class Player {
	private ReversiColor color;
	private int turn = 0;

	private Player() { }

	protected Player(ReversiColor c) {
		color = c;
	}

	public ReversiColor color() {
		return color;
	}

	public int turn() {
		return turn;
	}

	public void incrementTurn() {
		turn++;
	}
}
