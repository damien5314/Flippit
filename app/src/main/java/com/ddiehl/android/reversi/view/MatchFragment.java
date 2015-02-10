package com.ddiehl.android.reversi.view;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

public abstract class MatchFragment extends Fragment {
    private static final String TAG = MatchFragment.class.getSimpleName();

    protected TableLayout mMatchGridView;
    protected TextView mPlayerOneLabelTextView, mPlayerTwoLabelTextView;
    protected TextView mPlayerOneScoreTextView, mPlayerTwoScoreTextView;
    protected ImageView mTurnIndicator;
    protected View mBoardPanelView;
    protected Button mStartNewMatchButton, mSelectMatchButton;
    protected Dialog mDisplayedDialog;

    protected View mMatchMessageView;
    protected TextView mMatchMessageTextView;
    protected ImageView mMatchMessageIcon1, mMatchMessageIcon2;
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reversi, container, false);

        mBoardPanelView = v.findViewById(R.id.board_panels);
        mMatchGridView = (TableLayout) v.findViewById(R.id.match_grid);
        mMatchGridView.setVisibility(View.GONE);
        mPlayerOneLabelTextView = (TextView) v.findViewById(R.id.label_p1);
        mPlayerTwoLabelTextView = (TextView) v.findViewById(R.id.label_p2);
        mPlayerOneScoreTextView = (TextView) v.findViewById(R.id.score_p1);
        mPlayerTwoScoreTextView = (TextView) v.findViewById(R.id.score_p2);
        mTurnIndicator = (ImageView) v.findViewById(R.id.turn_indicator);
        mMatchMessageView = v.findViewById(R.id.match_message);
        mMatchMessageTextView = (TextView) v.findViewById(R.id.match_message_text);
        mMatchMessageIcon1 = (ImageView) v.findViewById(R.id.match_message_icon_1);
        mMatchMessageIcon2 = (ImageView) v.findViewById(R.id.match_message_icon_2);

        mStartNewMatchButton = (Button) v.findViewById(R.id.board_panel_new_game);
        mStartNewMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewMatch();
            }
        });

        mSelectMatchButton = (Button) v.findViewById(R.id.board_panel_select_game);
        mSelectMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMatch();
            }
        });

        return v;
    }

    public abstract void startNewMatch();
    public abstract void selectMatch();
}