package com.ddiehl.android.flippit.game;


public class ReversiPiece {
	private ReversiColor c;

	private ReversiPiece() { }

	public ReversiPiece(ReversiColor c) {
		this.c = c;
	}

	public ReversiColor color() {
		return c;
	}

	public void flipColor() {
		if (c == ReversiColor.White)
			c = ReversiColor.Black;
		else
			c = ReversiColor.White;
	}
}
