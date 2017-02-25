package com.ddiehl.android.reversi.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ddiehl.android.reversi.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class LauncherFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_launcher, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @OnClick(R.id.button_start_1p)
    void onSinglePlayerClicked() {
        Intent intent = new Intent(getActivity(), SinglePlayerMatchActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.button_start_mp)
    void onMultiPlayerClicked() {
        Intent intent = new Intent(getActivity(), MultiPlayerMatchActivity.class);
        startActivity(intent);
    }
}
