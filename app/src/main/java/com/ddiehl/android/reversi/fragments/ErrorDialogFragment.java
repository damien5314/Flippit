package com.ddiehl.android.reversi.fragments;


import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.ddiehl.android.reversi.activities.MultiPlayerMatchActivity;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class ErrorDialogFragment extends DialogFragment {
	private static final String KEY_RESOLVING_ERROR = "resolving_error";

	public ErrorDialogFragment() { }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Get the error code and retrieve the appropriate dialog
		int errorCode = this.getArguments().getInt(KEY_RESOLVING_ERROR);
		return GooglePlayServicesUtil.getErrorDialog(errorCode, this.getActivity(),
				MultiPlayerMatchActivity.RC_RESOLVE_ERROR);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		((MultiPlayerMatchActivity) getActivity()).onDialogDismissed();
	}
}
