package com.ddiehl.android.reversi.model

enum class GameState {
    NOT_STARTED,
    LIGHT_TURN,
    DARK_TURN,
    LIGHT_WIN,
    DARK_WIN,
    MATCH_CANCELLED,
    LIGHT_FORFEIT,
    DARK_FORFEIT
}
