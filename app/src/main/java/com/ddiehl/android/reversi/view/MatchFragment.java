package com.ddiehl.android.reversi.view;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.game.Board;
import com.jakewharton.rxbinding.view.RxView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.functions.Action1;

public abstract class MatchFragment extends Fragment {

    private static final String TAG = MatchFragment.class.getSimpleName();

    @BindView(R.id.match_grid)
    protected TableLayout mMatchGridView;
    @BindView(R.id.label_p1)
    protected TextView mPlayerOneLabelTextView;
    @BindView(R.id.label_p2)
    protected TextView mPlayerTwoLabelTextView;
    @BindView(R.id.score_p1)
    protected TextView mPlayerOneScoreTextView;
    @BindView(R.id.score_p2)
    protected TextView mPlayerTwoScoreTextView;
    @BindView(R.id.turn_indicator)
    protected ImageView mTurnIndicator;
    @BindView(R.id.board_panels)
    protected View mBoardPanelView;
    @BindView(R.id.board_panel_new_game)
    protected Button mStartNewMatchButton;
    @BindView(R.id.board_panel_select_game)
    protected Button mSelectMatchButton;
    @BindView(R.id.match_message)
    protected View mMatchMessageView;
    @BindView(R.id.match_message_text)
    protected TextView mMatchMessageTextView;
    @BindView(R.id.match_message_icon_1)
    protected ImageView mMatchMessageIcon1;
    @BindView(R.id.match_message_icon_2)
    protected ImageView mMatchMessageIcon2;

    protected Dialog mDisplayedDialog;

    protected Animation mLeftFadeOut, mLeftFadeIn, mRightFadeOut, mRightFadeIn;
    protected boolean mMatchMessageIcon1Color = false;
    protected boolean mMatchMessageIcon2Color = true;

    protected Board mBoard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reversi, container, false);
        ButterKnife.bind(this, view);

        initMatchGrid(mMatchGridView);
        mMatchGridView.setVisibility(View.GONE);

        return view;
    }

    @OnClick()
    void onStartNewMatchClicked() {
        startNewMatch();
    }

    @OnClick(R.id.board_panel_select_game)
    void onSelectMatchClicked() {
        selectMatch();
    }

    protected void initMatchGrid(ViewGroup grid) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            ViewGroup row = (ViewGroup) grid.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                View space = row.getChildAt(j);

                RxView.clicks(space)
                        .subscribe(onSpaceClicked(i, j));
            }
        }
    }

    private Action1<Void> onSpaceClicked(final int row, final int col) {
        return new Action1<Void>() {
            @Override
            public void call(Void v) {
                Log.d(TAG, "Piece clicked @ " + row + " " + col);
                handleSpaceClick(row, col);
            }
        };
    }

    abstract void startNewMatch();
    abstract void selectMatch();
    abstract void handleSpaceClick(int row, int col);
}
