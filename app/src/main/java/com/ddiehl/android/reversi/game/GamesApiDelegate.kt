package com.ddiehl.android.reversi.game

import com.google.android.gms.common.api.GoogleApiClient

interface GamesApiDelegate {

    val isConnected: Boolean
    fun connect()
    fun disconnect()

    class Impl(private val client: GoogleApiClient): GamesApiDelegate {
        override val isConnected: Boolean
            get() = client.isConnected

        override fun connect() = client.connect()

        override fun disconnect() = client.disconnect()
    }
}
