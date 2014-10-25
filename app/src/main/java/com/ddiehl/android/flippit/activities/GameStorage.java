package com.ddiehl.android.flippit.activities;


import android.content.Context;

import com.ddiehl.android.flippit.game.Board;
import com.ddiehl.android.flippit.game.BoardSpace;
import com.ddiehl.android.flippit.game.ReversiColor;
import com.ddiehl.android.flippit.utils.BoardIterator;

public class GameStorage {
    private static final String TAG = GameStorage.class.getSimpleName();

    public static void deserialize(Context ctx, String in) {
        Board _instance = Board.getInstance(ctx);
        int index = 0;
        for (int y = 0; y < _instance.height(); y++) {
            for (int x = 0; x < _instance.width(); x++) {
                char c = in.charAt(index++);
                _instance.setSpace(x, y, new BoardSpace(ctx, x, y));
                switch (c) {
                    case '0':
                        break;
                    case '1':
                        _instance.getSpaceAt(x, y).setColor(ReversiColor.White);
                        break;
                    case '2':
                        _instance.getSpaceAt(x, y).setColor(ReversiColor.Black);
                }
            }
        }
    }

    public static String serialize(Board board) {
        StringBuilder sb = new StringBuilder();
        BoardIterator i = new BoardIterator(board);
        while (i.hasNext()) {
            BoardSpace s = i.next();
            if (!s.isOwned())
                sb.append("0");
            else if (s.getColor() == ReversiColor.White)
                sb.append("1");
            else
                sb.append("2");
        }
        return sb.toString();
    }
}
