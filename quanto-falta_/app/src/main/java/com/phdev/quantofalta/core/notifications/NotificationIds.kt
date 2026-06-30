package com.phdev.quantofalta.core.notifications

import java.nio.ByteBuffer
import java.security.MessageDigest

object NotificationIds {
    fun fromKey(key: String): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return ByteBuffer.wrap(digest).int and Int.MAX_VALUE
    }
}
