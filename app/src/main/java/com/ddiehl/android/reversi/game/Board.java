package com.ddiehl.android.reversi.game;


import android.content.Context;

public class Board {
    private static final String TAG = Board.class.getSimpleName();
	private static Board _instance = null;
    private static Context ctx;
	private BoardSpace[][] spaces;
	private int width;
	private int height;

    private final byte[][] moveDirections = new byte[][] {
            {0,  -1}, // Down
            {1,  0}, // Right
            {-1, 0}, // Left
            {0,  1}, // Up
            {-1, -1}, // Down-Left
            {1,  -1}, // Down-Right
            {-1, 1}, // Top-Left
            {1,  1} // Top-Right
    };

	private Board(Context c) {
        ctx = c;
		width = 8;
		height = 8;
        spaces = new BoardSpace[height][width];
		reset();
	}

    protected Board copy() {
        Board copy = new Board(ctx);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                copy.spaces[y][x] = spaces[y][x].copy(ctx);
            }
        }
        return copy;
    }

	public void reset() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				spaces[y][x] = new BoardSpace(ctx, x, y);
			}
		}
		spaces[3][3].setColorNoAnimation(ReversiColor.White);
		spaces[3][4].setColorNoAnimation(ReversiColor.Black);
		spaces[4][4].setColorNoAnimation(ReversiColor.White);
		spaces[4][3].setColorNoAnimation(ReversiColor.Black);
	}

	public boolean hasMove(ReversiColor c) {
		BoardIterator i = new BoardIterator(this);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned())
				continue;
			for (byte[] move : moveDirections) {
				int value = moveValueInDirection(s, move[0], move[1], c);
				if (value != 0) {
					return true;
				}
			}
		}
		return false;
	}

	public BoardSpace getSpaceAt(int x, int y) {
		if (x >= 0 && x < width && y >= 0 && y < height)
			return spaces[y][x];

		return null;
	}

    public void setSpace(int x, int y, BoardSpace s) {
        this.spaces[y][x] = s;
    }

	public boolean commitPiece(BoardSpace space, ReversiColor playerColor) {
		for (byte[] move : moveDirections) {
			if (moveValueInDirection(space, move[0], move[1], playerColor) != 0) {
				flipInDirection(space, move[0], move[1], playerColor);
			}
		}
		return true;
	}

	public int spacesCapturedWithMove(BoardSpace s, ReversiColor playerColor) {
		int moveVal = 0;
		for (byte[] move : moveDirections)
			moveVal += moveValueInDirection(s, move[0], move[1], playerColor);
		return moveVal;
	}

	public int moveValueInDirection(BoardSpace s, int dx, int dy, ReversiColor playerColor) {
		if (s.x+dx < 0 || s.x+dx >= width || s.y+dy < 0 || s.y+dy >= height)
			return 0;

		int moveVal = 0;
		ReversiColor opponentColor = (playerColor == ReversiColor.Black) ? ReversiColor.White : ReversiColor.Black;
        BoardSpace firstPiece = getSpaceAt(s.x + dx, s.y + dy);

		if (firstPiece != null && firstPiece.getColor() == opponentColor) {
			int cx = s.x+dx;
			int cy = s.y+dy;
			while (getSpaceAt(cx, cy) != null && getSpaceAt(cx, cy).getColor() == opponentColor) {
				moveVal++;
				cx += dx;
				cy += dy;
			}
			if (getSpaceAt(cx, cy) != null && getSpaceAt(cx, cy).getColor() == playerColor) {
				return moveVal;
			}
		}

		return 0;
	}

    public void flipInDirection(BoardSpace s, int dx, int dy, ReversiColor playerColor) {
        s.setColorAnimated(playerColor);
        int cx = s.x + dx;
        int cy = s.y + dy;

        while (getSpaceAt(cx, cy).getColor() != playerColor) {
            getSpaceAt(cx, cy).flipColor();
            cx += dx;
            cy += dy;
        }
    }

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public static Board getInstance(Context c) {
		if (_instance == null)
			_instance = new Board(c);

		return _instance;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append("\n");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
                BoardSpace s = getSpaceAt(x, y);
                if (!s.isOwned())
                    sb.append("0 ");
				else if (s.getColor() == ReversiColor.White)
					sb.append("1 ");
				else
					sb.append("2 ");
			}
			if (y != spaces.length-1)
				sb.append("\n");
		}
		return sb.toString();
	}
}
