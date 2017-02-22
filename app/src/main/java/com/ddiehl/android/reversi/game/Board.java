package com.ddiehl.android.reversi.game;


import com.ddiehl.android.reversi.exceptions.IllegalMoveException;

import java.util.Iterator;
import java.util.NoSuchElementException;

import rx.Observable;
import rx.functions.Func0;

public class Board {

    private final BoardSpace[][] spaces;
    private final int columns;
    private final int rows;

    private final byte[][] MOVE_DIRECTIONS = new byte[][] {
            {0,  -1}, // Down
            {1,  0}, // Right
            {-1, 0}, // Left
            {0,  1}, // Up
            {-1, -1}, // Down-Left
            {1,  -1}, // Down-Right
            {-1, 1}, // Top-Left
            {1,  1} // Top-Right
    };

    public Board(int rows, int columns) {
        this.columns = columns;
        this.rows = rows;
        spaces = new BoardSpace[rows][columns];
        reset();
    }

    public Board(int rows, int cols, byte[] in) {
        this(rows, cols);

        int index = 0;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                byte c = in[index++];

                switch (c) {
                    case 0:
                        break;
                    case 1:
                        getSpaceAt(x, y).setColor(ReversiColor.Light);
                        break;
                    case 2:
                        getSpaceAt(x, y).setColor(ReversiColor.Dark);
                }
            }
        }
    }

    public Board(int rows, int cols, String in) {
        this(rows, cols);

        int index = 0;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                char c = in.charAt(index++);

                switch (c) {
                    case '0':
                        break;
                    case '1':
                        getSpaceAt(x, y).setColor(ReversiColor.Light);
                        break;
                    case '2':
                        getSpaceAt(x, y).setColor(ReversiColor.Dark);
                }
            }
        }
    }

    public Board copy() {
        Board copy = new Board(rows, columns);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                copy.spaces[y][x] = spaces[y][x].copy();
            }
        }
        return copy;
    }

    public void reset() {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                spaces[y][x] = new BoardSpace(x, y);
            }
        }
        spaces[3][3].setColor(ReversiColor.Light);
        spaces[3][4].setColor(ReversiColor.Dark);
        spaces[4][4].setColor(ReversiColor.Light);
        spaces[4][3].setColor(ReversiColor.Dark);
    }

    public boolean hasMove(ReversiColor c) {
        BoardIterator i = iterator();
        while (i.hasNext()) {
            BoardSpace s = i.next();
            if (s.isOwned())
                continue;
            for (byte[] move : MOVE_DIRECTIONS) {
                int value = moveValueInDirection(s, move[0], move[1], c);
                if (value != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    BoardSpace getSpaceAt(int x, int y) {
        if (x >= 0 && x < columns && y >= 0 && y < rows)
            return spaces[y][x];

        return null;
    }

    public Observable<Boolean> requestClaimSpace(final int x, final int y, final ReversiColor color) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BoardSpace space = spaces[x][y];

                // If space is already claimed, return an error
                if (space.isOwned()) {
                    return Observable.error(new IllegalMoveException("space is already owned"));
                }

                int captured = spacesCapturedWithMove(space, color);
                if (captured <= 0) {
                    return Observable.error(new IllegalMoveException("move value is <= 0: " + captured));
                }

                commitPiece(space, color);
                return Observable.just(true);
            }
        });
    }

    public void commitPiece(BoardSpace space, ReversiColor playerColor) {
        for (byte[] move : MOVE_DIRECTIONS) {
            if (moveValueInDirection(space, move[0], move[1], playerColor) != 0) {
                flipInDirection(space, move[0], move[1], playerColor);
            }
        }
    }

    public int spacesCapturedWithMove(BoardSpace s, ReversiColor playerColor) {
        int moveVal = 0;
        for (byte[] move : MOVE_DIRECTIONS)
            moveVal += moveValueInDirection(s, move[0], move[1], playerColor);
        return moveVal;
    }

    private int moveValueInDirection(BoardSpace s, int dx, int dy, ReversiColor playerColor) {
        if (s.x()+dx < 0 || s.x()+dx >= columns || s.y()+dy < 0 || s.y()+dy >= rows)
            return 0;

        int moveVal = 0;
        ReversiColor opponentColor = (playerColor == ReversiColor.Dark) ? ReversiColor.Light : ReversiColor.Dark;
        BoardSpace firstPiece = getSpaceAt(s.x() + dx, s.y() + dy);

        if (firstPiece != null && firstPiece.getColor() == opponentColor) {
            int cx = s.x()+dx;
            int cy = s.y()+dy;
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
        s.setColor(playerColor);
        int cx = s.x() + dx;
        int cy = s.y() + dy;

        while (getSpaceAt(cx, cy).getColor() != playerColor) {
            getSpaceAt(cx, cy).flipColor();
            cx += dx;
            cy += dy;
        }
    }

    public int getNumSpacesForColor(ReversiColor c) {
        int count = 0;
        BoardIterator i = iterator();
        while (i.hasNext()) {
            BoardSpace s = i.next();
            if (s.isOwned() && s.getColor() == c)
                count++;
        }
        return count;
    }

    public int getNumberOfEmptySpaces() {
        int count = 0;
        BoardIterator i = iterator();
        while (i.hasNext()) {
            BoardSpace s = i.next();
            if (!s.isOwned())
                count++;
        }
        return count;
    }

    public int width() {
        return columns;
    }

    public int height() {
        return rows;
    }

    public byte[] serialize() {
        byte[] out = new byte[64];
        int index = 0;
        BoardIterator i = iterator();
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
        return (byte) (s.y() * 8 + s.x() + 1);
    }

    public BoardSpace getBoardSpaceFromNum(int n) {
        n -= 1;
        return getSpaceAt(n % 8, n / 8);
    }

    public BoardIterator iterator() {
        return new BoardIterator(this);
    }

    public BoardSpace spaceAt(int row, int col) {
        return spaces[row][col];
    }

    static class BoardIterator implements Iterator<BoardSpace> {
        private Board mBoard;
        private int x = 0;
        private int y = 0;

        public BoardIterator(Board b) {
            mBoard = b;
        }

        @Override
        public boolean hasNext() {
            return y != mBoard.height();
        }

        @Override
        public BoardSpace next() {
            if (!hasNext()) throw new NoSuchElementException();

            BoardSpace s = mBoard.getSpaceAt(x, y);
            if (++x == mBoard.width()) {
                y++; x = 0;
            }

            return s;
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
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
