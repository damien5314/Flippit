package com.ddiehl.android.flippit.game;


public class BoardSpace {
	private ReversiPiece piece;
	protected int x, y;

	private BoardSpace() { }

	protected BoardSpace(int x, int y) {
		piece = null;
		this.x = x;
		this.y = y;
	}

	public ReversiPiece piece() {
		return piece;
	}

	public void setPiece(ReversiPiece p) {
		piece = p;
	}

}
