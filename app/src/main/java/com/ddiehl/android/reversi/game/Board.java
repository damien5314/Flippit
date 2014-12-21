package com.ddiehl.android.reversi.game;


import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.activities.MatchActivity;

public class Board {
    private static final String TAG = Board.class.getSimpleName();
    private static Context ctx;
	private final BoardSpace[][] spaces;
	private final int width;
	private final int height;

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

	public Board(Context c) {
        ctx = c;
		width = 8;
		height = 8;
        spaces = new BoardSpace[height][width];
		reset();
	}

    public Board copy() {
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
		spaces[3][3].setColorNoAnimation(ReversiColor.Light);
		spaces[3][4].setColorNoAnimation(ReversiColor.Dark);
		spaces[4][4].setColorNoAnimation(ReversiColor.Light);
		spaces[4][3].setColorNoAnimation(ReversiColor.Dark);
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

	public void commitPiece(BoardSpace space, ReversiColor playerColor) {
		for (byte[] move : moveDirections) {
			if (moveValueInDirection(space, move[0], move[1], playerColor) != 0) {
				flipInDirection(space, move[0], move[1], playerColor);
			}
		}
	}

	public int spacesCapturedWithMove(BoardSpace s, ReversiColor playerColor) {
		int moveVal = 0;
		for (byte[] move : moveDirections)
			moveVal += moveValueInDirection(s, move[0], move[1], playerColor);
		return moveVal;
	}

	private int moveValueInDirection(BoardSpace s, int dx, int dy, ReversiColor playerColor) {
		if (s.x+dx < 0 || s.x+dx >= width || s.y+dy < 0 || s.y+dy >= height)
			return 0;

		int moveVal = 0;
		ReversiColor opponentColor = (playerColor == ReversiColor.Dark) ? ReversiColor.Light : ReversiColor.Dark;
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

    private void flipInDirection(BoardSpace s, int dx, int dy, ReversiColor playerColor) {
        s.setColorAnimated(playerColor);
        int cx = s.x + dx;
        int cy = s.y + dy;

        while (getSpaceAt(cx, cy).getColor() != playerColor) {
            getSpaceAt(cx, cy).flipColor();
            cx += dx;
            cy += dy;
        }
    }

	public int getNumSpacesForColor(ReversiColor c) {
		int count = 0;
		BoardIterator i = new BoardIterator(this);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (s.isOwned() && s.getColor() == c)
				count++;
		}
		return count;
	}

	public int getNumberOfEmptySpaces() {
		int count = 0;
		BoardIterator i = new BoardIterator(this);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (!s.isOwned())
				count++;
		}
		return count;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public void deserialize(byte[] in) {
		int index = 0;
		for (int y = 0; y < height(); y++) {
			for (int x = 0; x < width(); x++) {
				byte c = in[index++];
				setSpace(x, y, new BoardSpace(ctx, x, y));
				switch (c) {
					case 0:
						break;
					case 1:
						getSpaceAt(x, y).setColorNoAnimation(ReversiColor.Light);
						break;
					case 2:
						getSpaceAt(x, y).setColorNoAnimation(ReversiColor.Dark);
				}
			}
		}
	}

	public void deserialize(String in) {
		int index = 0;
		for (int y = 0; y < height(); y++) {
			for (int x = 0; x < width(); x++) {
				char c = in.charAt(index++);
				setSpace(x, y, new BoardSpace(ctx, x, y));
				switch (c) {
					case '0':
						break;
					case '1':
						getSpaceAt(x, y).setColorNoAnimation(ReversiColor.Light);
						break;
					case '2':
						getSpaceAt(x, y).setColorNoAnimation(ReversiColor.Dark);
				}
			}
		}
	}

	public byte[] serialize() {
		byte[] out = new byte[64];
		int index = 0;
		BoardIterator i = new BoardIterator(this);
		while (i.hasNext()) {
			BoardSpace s = i.next();
			if (!s.isOwned())
				out[index++] = 0;
			else if (s.getColor() == ReversiColor.Light)
				out[index++] = 1;
			else
				out[index++] = 2;
		}
		return out;
	}

	public byte getSpaceNumber(BoardSpace s) {
		return (byte) (s.y * 8 + s.x + 1);
	}

	public BoardSpace getBoardSpaceFromNum(int n) {
		n -= 1;
		return getSpaceAt(n % 8, n / 8);
	}

	public void displayBoard(final MatchActivity a) {
		a.findViewById(R.id.board_panels).setVisibility(View.GONE);
		TableLayout grid = (TableLayout) a.findViewById(R.id.match_grid);
		grid.setVisibility(View.GONE); // Hide the view until we finish adding children
		grid.removeAllViews();

		grid.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 16));
//		grid.setWeightSum(8);

//		int bHeight = (int) a.getResources().getDimension(R.dimen.space_row_height);
		int bMargin = (int) a.getResources().getDimension(R.dimen.space_padding);

		for (int y = 0; y < height(); y++) {
			TableRow row = new TableRow(a);
			row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 0, 1));
//			row.setWeightSum(width());
			for (int x = 0; x < width(); x++) {
				BoardSpace space = getSpaceAt(x, y);
				TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1);
				params.setMargins(bMargin, bMargin, bMargin, bMargin);
				space.setLayoutParams(params);
				space.setOnClickListener(
						new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								a.claim((BoardSpace) v);
							}
						});
				row.addView(space);
			}
			grid.addView(row);
		}
		grid.setVisibility(View.VISIBLE);
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
				else if (s.getColor() == ReversiColor.Light)
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
