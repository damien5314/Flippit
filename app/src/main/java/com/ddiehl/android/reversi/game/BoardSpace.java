package com.ddiehl.android.reversi.game;


import android.support.annotation.Nullable;

public class BoardSpace {

    private @Nullable ReversiColor color;
    private int x;
    private int y;

    public BoardSpace() {
        color = null;
    }

    public BoardSpace(int x, int y) {
        color = null;
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    protected BoardSpace copy() {
        BoardSpace copy = new BoardSpace(x, y);
        copy.color = color;
        return copy;
    }

    public boolean isOwned() {
        return color != null;
    }

    public @Nullable ReversiColor getColor() {
        return color;
    }

    public void setColor(@Nullable ReversiColor c) {
        color = c;
    }

    public ReversiColor flipColor() {
        if (color == null) {
            return null;
        } else {
            color = (color == ReversiColor.Dark) ? ReversiColor.Light : ReversiColor.Dark;
            return color;
        }
    }
}
