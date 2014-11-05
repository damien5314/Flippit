package com.ddiehl.android.reversi.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ddiehl.android.reversi.R;
import com.ddiehl.android.reversi.activities.MultiplayerGameSelectionActivity;

import java.util.ArrayList;


public class MatchSelectionAdapter extends ArrayAdapter<String> {
	private MultiplayerGameSelectionActivity ctx;
	private int layoutResourceId;
	private ArrayList<String> mList;

	public MatchSelectionAdapter(Context c, int resource, ArrayList<String> list) {
		super(c, resource, list);
		ctx = (MultiplayerGameSelectionActivity) c;
		layoutResourceId = resource;
		mList = list;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null)
			row = ctx.getLayoutInflater().inflate(layoutResourceId, parent, false);

		String opponentName = mList.get(position);
		ItemHolder h = new ItemHolder();
		h.itemText = (TextView) row.findViewById(R.id.itemText);
		h.itemText.setText(opponentName);
		row.setTag(h);

		return row;
	}

	static class ItemHolder {
		TextView itemText;
	}

}
