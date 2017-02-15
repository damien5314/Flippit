package com.ddiehl.android.reversi.game;


import java.util.Iterator;
import java.util.NoSuchElementException;

public class Board {

    private final BoardSpace[][] spaces;
    private final int width;
    private final int height;

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

    public Board() {
        width = 8;
        height = 8;
        spaces = new BoardSpace[height][width];
        reset();
    }

    public Board copy() {
        Board copy = new Board();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                copy.spaces[y][x] = spaces[y][x].copy();
            }
        }
        return copy;
    }

    public void reset() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                spaces[y][x] = new BoardSpace(x, y);
            }
        }
        spaces[3][3].setColorNoAnimation(ReversiColor.Light);
        spaces[3][4].setColorNoAnimation(ReversiColor.Dark);
        spaces[4][4].setColorNoAnimation(ReversiColor.Light);
        spaces[4][3].setColorNoAnimation(ReversiColor.Dark);
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
        if (x >= 0 && x < width && y >= 0 && y < height)
            return spaces[y][x];

        return null;
    }

    void setSpace(int x, int y, BoardSpace s) {
        this.spaces[y][x] = s;
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
                setSpace(x, y, new BoardSpace(x, y));
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
                setSpace(x, y, new BoardSpace(x, y));
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
        return (byte) (s.y * 8 + s.x + 1);
    }

    public BoardSpace getBoardSpaceFromNum(int n) {
        n -= 1;
        return getSpaceAt(n % 8, n / 8);
    }

    public BoardIterator iterator() {
        return new BoardIterator(this);
    }

    private static class BoardIterator implements Iterator<BoardSpace> {
        private Board mBoard;
        private int x;
        private int y;

        public BoardIterator(Board b) {
            mBoard = b;
            x = 0; y = 0;
        }

        @Override
        public boolean hasNext() {
            return y != mBoard.height();
        }

        @Override
        public BoardSpace next() {
            if (!hasNext())
                throw new NoSuchElementException();

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
