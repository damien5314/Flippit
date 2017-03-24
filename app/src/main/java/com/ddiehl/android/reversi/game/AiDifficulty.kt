package com.ddiehl.android.reversi.game

import android.support.annotation.StringRes
import com.ddiehl.android.reversi.R

enum class AiDifficulty(@StringRes val nameResId: Int, val value: Int) {
    EASY(R.string.ai_difficulty_easy, 1),
    HARD(R.string.ai_difficulty_hard, 2);

    companion object {
        fun valueOf(value: Int): AiDifficulty
                = AiDifficulty.values().single { it.value == value }
    }
}
