package com.ddiehl.android.reversi.utils;


import android.content.Context;

import com.ddiehl.android.reversi.game.Board;
import com.ddiehl.android.reversi.game.BoardSpace;
import com.ddiehl.android.reversi.game.ReversiColor;

public class GameStorage {
    private static final String TAG = GameStorage.class.getSimpleName();

    public static void deserialize(Context ctx, byte[] in) {
        Board _instance = Board.getInstance(ctx);
        int index = 0;
        for (int y = 0; y < _instance.height(); y++) {
            for (int x = 0; x < _instance.width(); x++) {
                byte c = in[index++];
                _instance.setSpace(x, y, new BoardSpace(ctx, x, y));
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        _instance.getSpaceAt(x, y).setColorNoAnimation(ReversiColor.White);
                        break;
                    case 2:
                        _instance.getSpaceAt(x, y).setColorNoAnimation(ReversiColor.Black);
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
}
