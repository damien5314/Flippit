package com.ddiehl.android.flippit.game;


import android.content.Context;
import android.widget.Button;

public class BoardSpace extends Button {
	private ReversiPiece piece;
	protected int x, y;

	private BoardSpace(Context c) {
        super(c);
    }

    public BoardSpace(Context c, int x, int y) {
		super(c);
        piece = null;
        this.x = x;
        this.y = y;
    }

    public boolean hasPiece() {
        return piece != null;
    }

	public ReversiPiece piece() {
		return piece;
	}

	public void setPiece(ReversiPiece p) {
		piece = p;
	}

}
