package com.ddiehl.android.reversi.game;


import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.ddiehl.android.reversi.R;

public class BoardSpace extends Button {
    private Context ctx;
    private ReversiColor color;
	public int x, y;

    public BoardSpace(Context c, int x, int y) {
		super(c);
        ctx = c;
        setBackgroundResource(R.drawable.board_space_neutral);
        color = null;
        this.x = x;
        this.y = y;
    }

    protected BoardSpace copy(Context context) {
        BoardSpace copy = new BoardSpace(context, x, y);
        copy.color = color;
        return copy;
    }

    public boolean isOwned() {
        return color != null;
    }

    public ReversiColor getColor() {
        return color;
    }

    public void setColorNoAnimation(ReversiColor c) {
        color = c;
        updateBackgroundResource();
    }

    public void setColorAnimated(ReversiColor c) {
        color = c;
        animateBackgroundChange();
    }

    public void flipColor() {
        color = (color == ReversiColor.Black) ? ReversiColor.White : ReversiColor.Black;
        animateBackgroundChange();
    }

    private void animateBackgroundChange() {
        final Animation fadeOut = AnimationUtils.loadAnimation(ctx, R.anim.anim_fadeout);
        final Animation fadeIn = AnimationUtils.loadAnimation(ctx, R.anim.anim_fadein);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                updateBackgroundResource();
                startAnimation(fadeIn);
            }
        });
        startAnimation(fadeOut);
    }

    private void updateBackgroundResource() {
        setBackgroundResource((color == ReversiColor.White)
                ? R.drawable.board_space_p1 : R.drawable.board_space_p2);
    }
}
