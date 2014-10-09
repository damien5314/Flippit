package com.ddiehl.android.flippit;


public class ReversiPiece {
	private Reversi.Color c;

	private ReversiPiece() { }

	public ReversiPiece(Reversi.Color c) {
		this.c = c;
	}

	public Reversi.Color color() {
		return c;
	}

	public void flipColor() {
		if (c == Reversi.Color.White)
			c = Reversi.Color.Black;
		else
			c = Reversi.Color.White;
	}
}
