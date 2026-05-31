package com.xpenseledger.app.notification.service

import java.security.MessageDigest

/**
 * Computes a deterministic hash for a detected transaction.
 * Used to prevent duplicate entries when the same notification is posted twice.
 */
object DuplicateDetector {

    /**
     * Produces a short hex hash from amount + epoch-second + packageName.
     * Timestamp is rounded to the nearest 10-second window so minor OS
     * re-delivery jitter is absorbed.
     */
    fun computeHash(amount: Double, timestampMillis: Long, packageName: String): String {
        val window = (timestampMillis / 10_000L) // 10-second buckets
        val raw    = "$amount|$window|$packageName"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }
}

