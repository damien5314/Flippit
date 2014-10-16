package com.ddiehl.android.flippit.game;


import android.content.Context;
import android.util.Log;

public class Board {
    private static final String TAG = Board.class.getSimpleName();
	private static Board _instance = null;
    private static Context context;
	private BoardSpace[][] spaces;
	private int width;
	private int height;

    private final int[][] moveDirections = new int[][] {
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
        context = c;
		width = 8;
		height = 8;
        spaces = new BoardSpace[height][width];
		reset();
	}

	public void reset() {
		for (int y = 0; y < spaces.length; y++) {
			for (int x = 0; x < spaces[0].length; x++) {
				spaces[y][x] = new BoardSpace(context, x, y);
			}
		}

		spaces[3][3].setColor(ReversiColor.White);
		spaces[3][4].setColor(ReversiColor.Black);
		spaces[4][4].setColor(ReversiColor.White);
		spaces[4][3].setColor(ReversiColor.Black);
	}

	public boolean hasMove(Player p) {
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                BoardSpace s = getSpaceAt(x, y);
                if (s.isOwned())
                    continue;
                for (int[] move : moveDirections) {
                    int value = moveValueInDirection(s, move[0], move[1], p.getColor());
                    if (value != 0) {
//                        Log.d(TAG, "moveValueInDirection((" + s.x + "," + s.y + ")," + move[0]
//                                + "," + move[1] + "," + p.getColor() + ") = " + value);
                        return true;
                    }
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

	public boolean commitPiece(BoardSpace space, ReversiColor playerColor) {
		for (int[] move : moveDirections) {
			if (moveValueInDirection(space, move[0], move[1], playerColor) != 0) {
				flipInDirection(space, move[0], move[1], playerColor);
			}
		}
		return true;
	}

	public int moveValue(BoardSpace s, ReversiColor playerColor) {
		int moveVal = 0;
		for (int[] move : moveDirections)
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
        s.setColor(playerColor);
        int cx = s.x + dx;
        int cy = s.y + dy;

        while (getSpaceAt(cx, cy).getColor() != playerColor) {
            getSpaceAt(cx, cy).flipColor();
            cx += dx;
            cy += dy;
        }
    }

    public BoardSpace getBestMove_d1(Player p) {
        BoardSpace best = null;
        int bestVal = 0;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                BoardSpace space = getSpaceAt(x, y);
                if (!space.isOwned()) {
                    int val = moveValue(space, p.getColor());
                    if (val > bestVal) {
                        best = space;
                        bestVal = val;
                    }
                }
            }
        }

        if (best != null)
            Log.i(TAG, "Best move @(" + best.x + "," + best.y + ") has value of " + bestVal);

        return best;
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
