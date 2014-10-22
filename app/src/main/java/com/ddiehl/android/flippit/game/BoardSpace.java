package com.ddiehl.android.flippit.game;


import android.content.Context;
import android.widget.Button;

import com.ddiehl.android.flippit.R;

public class BoardSpace extends Button {
    private ReversiColor color;
	public int x, y;

	private BoardSpace(Context c) {
        super(c);
    }

    public BoardSpace(Context c, int x, int y) {
		super(c);
        setBackgroundResource(R.drawable.board_space_neutral);
        color = null;
        this.x = x;
        this.y = y;
    }

    protected BoardSpace copy(Context context) {
        return new BoardSpace(context, x, y);
    }

    public boolean isOwned() {
        return color != null;
    }

	public void setColor(ReversiColor c) {
		color = c;
        updateBackgroundColor();
	}

    public ReversiColor getColor() {
        return color;
    }

    public void flipColor() {
        color = (color == ReversiColor.Black) ? ReversiColor.White : ReversiColor.Black;
        updateBackgroundColor();
    }

    private void updateBackgroundColor() {
        setBackgroundResource((color == ReversiColor.Black)
                ? R.drawable.board_space_p2 : R.drawable.board_space_p1);
//        setBackgroundColor( (color == ReversiColor.Black) ? Color.BLACK : Color.WHITE );
    }
}
