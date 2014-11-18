package com.ddiehl.android.reversi.game;


import android.content.Context;

public class GameStorage {
    private static final String TAG = GameStorage.class.getSimpleName();

    public static void deserialize(Context ctx, byte[] in) {
        Board board = Board.getInstance(ctx);
        int index = 0;
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                byte c = in[index++];
                board.setSpace(x, y, new BoardSpace(ctx, x, y));
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        board.getSpaceAt(x, y).setColorNoAnimation(ReversiColor.White);
                        break;
                    case 2:
                        board.getSpaceAt(x, y).setColorNoAnimation(ReversiColor.Black);
                }
            }
        }
    }

    public static byte[] serialize(Board board) {
		byte[] out = new byte[64];
		int index = 0;
        BoardIterator i = new BoardIterator(board);
        while (i.hasNext()) {
            BoardSpace s = i.next();
            if (!s.isOwned())
				out[index++] = 0;
            else if (s.getColor() == ReversiColor.White)
				out[index++] = 1;
            else
				out[index++] = 2;
        }
        return out;
    }

	public static byte getSpaceNumber(BoardSpace s) {
		return (byte) (s.y * 8 + s.x + 1);
	}

	public static BoardSpace getBoardSpaceFromNum(Board b, int n) {
		n -= 1;
		return b.getSpaceAt(n % 8, n / 8);
	}
}
