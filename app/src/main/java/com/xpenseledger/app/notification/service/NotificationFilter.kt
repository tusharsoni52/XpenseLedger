package com.xpenseledger.app.notification.service

/**
 * Stage 1 + pre-filter: decides whether a notification is worth parsing.
 *
 * Rejects:
 *  - Unknown packages (not in whitelist)
 *  - OTP / verification messages
 *  - Failed / declined / insufficient-balance messages
 *  - Promotional messages (no amount keyword)
 */
object NotificationFilter {

    private val OTP_KEYWORDS = listOf("otp", "one time password", "one-time password", "verification code")
    private val FAILURE_KEYWORDS = listOf(
        "failed", "failure", "declined", "unsuccessful", "insufficient",
        "not processed", "could not", "unable to", "reversed", "rejected"
    )
    private val AMOUNT_KEYWORDS = listOf("₹", "rs.", "rs ", "inr", "rupee")

    /**
     * Returns true when the notification should be forwarded to the parser.
     */
    fun shouldParse(packageName: String, title: String?, text: String?): Boolean {
        if (!MonitoredApps.isKnown(packageName)) return false

        val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()

        // Reject OTPs
        if (OTP_KEYWORDS.any { combined.contains(it) }) return false

        // Reject failures
        if (FAILURE_KEYWORDS.any { combined.contains(it) }) return false

        // Must have an amount indicator
        if (AMOUNT_KEYWORDS.none { combined.contains(it) }) return false

        return true
    }
}

