package com.whatsappclone.core.common.util

import java.util.UUID

object UuidGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
