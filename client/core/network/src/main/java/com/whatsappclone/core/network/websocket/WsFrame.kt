package com.whatsappclone.core.network.websocket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WsFrame(
    val event: String,
    val data: JsonElement? = null,
    val ref: String? = null
)
