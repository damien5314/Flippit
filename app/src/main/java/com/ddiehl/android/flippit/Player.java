package com.ddiehl.android.flippit;

public class Player {
	private Reversi.Color color;
	private int turn = 0;

	private Player() { }

	protected Player(Reversi.Color c) {
		color = c;
	}

	public Reversi.Color color() {
		return color;
	}

	public int turn() {
		return turn;
	}

	public void incrementTurn() {
		turn++;
	}
}
