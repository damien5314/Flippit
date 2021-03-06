package com.ddiehl.android.reversi.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.bindView
import com.ddiehl.android.reversi.R
import com.ddiehl.android.reversi.game.AiDifficulty
import com.ddiehl.android.reversi.getString
import com.ddiehl.android.reversi.toast

class SettingsView : FrameLayout {

    companion object {
        private val LAYOUT_RES_ID = R.layout.settings_view
    }

    val mDifficultySettingLayout by bindView<ViewGroup>(R.id.settings_difficulty)
    val mDifficultyValueText by bindView<TextView>(R.id.settings_difficulty_value)
    val mSignOutLayout by bindView<ViewGroup>(R.id.settings_sign_out)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context)
                .inflate(LAYOUT_RES_ID, this, true)
    }

    fun bindSettings(singlePlayerSettings: SinglePlayerSettings) {
        mDifficultyValueText.setText(singlePlayerSettings.aiDifficulty.nameResId)
        mDifficultySettingLayout.setOnClickListener {
            showDifficultySettingDialog(singlePlayerSettings)
        }
        mSignOutLayout.setOnClickListener {
            showDialogForSignOut()
        }
    }

    private fun showDifficultySettingDialog(settings: SinglePlayerSettings) {
        val entries = context.resources.getStringArray(R.array.pref_ai_difficulty_entries)
        val values = context.resources.getIntArray(R.array.pref_ai_difficulty_values)
        val selected = values.indexOfFirst { value -> value == settings.aiDifficulty.value }

        AlertDialog.Builder(context)
                .setSingleChoiceItems(entries, selected, {
                    dialog, which ->
                    // Get AiDifficulty selected in UI
                    val difficulty = AiDifficulty.valueOf(values[which])
                    settings.aiDifficulty = difficulty

                    // Rebind settings to UI
                    bindSettings(settings)

                    // Dismiss dialog
                    dialog.dismiss()
                })
                .show()
    }

    private fun showDialogForSignOut() {
        AlertDialog.Builder(context)
                .setTitle(getString(R.string.settings_dialog_sign_out_title))
                .setMessage(getString(R.string.settings_dialog_sign_out_message))
                .setPositiveButton(getString(R.string.settings_dialog_sign_out_confirm)) { _, _ ->
                    toast(R.string.sign_out_confirmation)
                    val activity = context as Activity
                    activity.setResult(SettingsActivity.RESULT_SIGN_OUT)
                    activity.finish()
                }
                .setNegativeButton(getString(R.string.settings_dialog_sign_out_cancel)) { _, _ -> }
                .setCancelable(true)
                .show()
    }
}
