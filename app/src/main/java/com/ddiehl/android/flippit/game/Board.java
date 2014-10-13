package com.ddiehl.android.flippit.game;


import android.content.Context;

public class Board {
    private static final String TAG = Board.class.getSimpleName();
	private static Board _instance = null;
    private static Context context;
	private BoardSpace[][] spaces;
	private int width;
	private int height;

    private final int[][] moveConfigurations = new int[][] {
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
                if (getSpaceAt(x,y).isOwned())
                    continue;
                for (int[] move : moveConfigurations)
                    if (checkMoveValid(x, y, move[0], move[1], p.getColor())) return true;
            }
        }
		return false;
	}

	public BoardSpace getSpaceAt(int x, int y) {
		if (x >= 0 && x < width && y >= 0 && y < height)
			return spaces[y][x];

		return null;
	}

	public boolean spaceExists(int x, int y) {
		return (getSpaceAt(x, y) != null);
	}

	public boolean setPieceOn(int x, int y, ReversiColor playerColor) {
		if (getSpaceAt(x,y).isOwned())
            return false;

        boolean valid = false;
        for (int[] move : moveConfigurations) {
            if (checkMoveValid(x, y, move[0], move[1], playerColor)) {
                flipColorsInDirection(x, y, move[0], move[1], playerColor);
                valid = true;
            }
        }

		return valid;
	}

	public boolean checkMoveValid(int x, int y, int dx, int dy, ReversiColor playerColor) {
		if (x+dx < 0 || x+dx >= width || y+dy < 0 || y+dy >= height)
			return false;

		ReversiColor opponentColor = (playerColor == ReversiColor.Black) ? ReversiColor.White : ReversiColor.Black;
        BoardSpace firstPiece = getSpaceAt(x + dx, y + dy);

		if (firstPiece != null && firstPiece.getColor() == opponentColor) {
			int cx = x+dx;
			int cy = y+dy;
			while (getSpaceAt(cx, cy) != null && getSpaceAt(cx, cy).getColor() == opponentColor) {
				cx += dx;
				cy += dy;
			}
			if (getSpaceAt(cx, cy) != null && getSpaceAt(cx, cy).getColor() == playerColor) {
				return true;
			}
		}

		return false;
	}

    public void flipColorsInDirection(int x, int y, int dx, int dy, ReversiColor playerColor) {
        int cx = x + dx;
        int cy = y + dy;

        while (getSpaceAt(cx, cy).getColor() != playerColor) {
            getSpaceAt(cx, cy).flipColor();
            cx += dx;
            cy += dy;
        }
        getSpaceAt(x,y).setColor(playerColor);
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
		for (int y = 0; y < spaces.length; y++) {
			for (int x = 0; x < spaces[0].length; x++) {
				if (!spaceExists(x, y))
					sb.append("0 ");
				else if (getSpaceAt(x, y).getColor() == ReversiColor.White)
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
