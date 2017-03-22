package com.ddiehl.android.reversi.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.bindView
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.getString

class SettingsView : FrameLayout {

    companion object {
        private val LAYOUT_RES_ID = R.layout.settings_view
    }

    val mDifficultyValueText by bindView<TextView>(R.id.settings_difficulty_value)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        LayoutInflater.from(context)
                .inflate(LAYOUT_RES_ID, this, true)
    }

    fun bindSettings(singlePlayerSettings: SinglePlayerSettings) {
        mDifficultyValueText.setText(singlePlayerSettings.aiDifficulty.nameResId)
    }

    private fun showDialogForSignOut() {
        AlertDialog.Builder(context)
                .setTitle(getString(R.string.settings_dialog_sign_out_title))
                .setMessage(getString(R.string.settings_dialog_sign_out_message))
                .setPositiveButton(getString(R.string.settings_dialog_sign_out_confirm)) { _, _ ->
                    (context as Activity).setResult(SettingsActivity.RESULT_SIGN_OUT)
                    (context as Activity).finish()
                }
                .setNegativeButton(getString(R.string.settings_dialog_sign_out_cancel)) { _, _ -> }
                .setCancelable(true)
                .show()
    }
}
