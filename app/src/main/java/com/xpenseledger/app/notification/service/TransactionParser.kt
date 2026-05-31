package com.xpenseledger.app.notification.service

import com.xpenseledger.app.notification.model.PendingTransaction

/**
 * Multi-stage parser that converts raw notification text into a [PendingTransaction].
 *
 * Stage 2 — Amount extraction
 * Stage 3 — Transaction type (EXPENSE / INCOME)
 * Stage 4 — Merchant / payee extraction (per-source patterns)
 * Stage 5 — Category assignment via [CategoryMatcher]
 */
object TransactionParser {

    // ── Amount patterns ────────────────────────────────────────────────────────
    // Matches: ₹450, ₹1,200.00, Rs.450, Rs 1,200, INR 500, Rs450
    private val AMOUNT_REGEX = Regex(
        """(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // ── Transaction type keywords ──────────────────────────────────────────────
    private val DEBIT_KEYWORDS  = listOf("debited","paid","spent","deducted","payment","sent","purchase")
    private val CREDIT_KEYWORDS = listOf("credited","received","deposited","refund","cashback","credit")

    // ── Per-source merchant patterns ───────────────────────────────────────────
    // Group 1 must capture the merchant/payee name
    private val GOOGLE_PAY_MERCHANT  = Regex("""paid to ([^.\n\r]+?)(?:\s+via|\.|$)""", RegexOption.IGNORE_CASE)
    private val PHONEPE_MERCHANT     = Regex("""sent to ([^.\n\r]+?)(?:\s*$|\.)""", RegexOption.IGNORE_CASE)
    private val PAYTM_MERCHANT       = Regex("""(?:paid|payment) (?:to|at) ([^.\n\r]+?)(?:\.|$)""", RegexOption.IGNORE_CASE)
    private val HDFC_MERCHANT        = Regex("""(?:at|to) ([A-Z][A-Za-z0-9 .&-]{2,30})""")
    private val ICICI_MERCHANT       = Regex("""(?:at|to) ([A-Z][A-Za-z0-9 .&-]{2,30})""")
    private val GENERIC_MERCHANT     = Regex("""(?:at|to|for) ([A-Za-z][A-Za-z0-9 .&-]{2,30})""", RegexOption.IGNORE_CASE)

    data class ParseResult(
        val amount: Double,
        val merchant: String,
        val transactionType: String   // "EXPENSE" or "INCOME"
    )

    fun parse(packageName: String, title: String?, text: String?): ParseResult? {
        val fullText = "${title.orEmpty()} ${text.orEmpty()}"
        if (fullText.isBlank()) return null

        val amount = extractAmount(fullText) ?: return null
        val type   = detectType(fullText)
        val merchant = extractMerchant(packageName, fullText)

        return ParseResult(amount = amount, merchant = merchant, transactionType = type)
    }

    // ── Stage 2: Amount ────────────────────────────────────────────────────────
    private fun extractAmount(text: String): Double? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    // ── Stage 3: Type ──────────────────────────────────────────────────────────
    private fun detectType(text: String): String {
        val lower = text.lowercase()
        if (CREDIT_KEYWORDS.any { lower.contains(it) }) return "INCOME"
        return "EXPENSE"
    }

    // ── Stage 4: Merchant ──────────────────────────────────────────────────────
    private fun extractMerchant(packageName: String, text: String): String {
        val pattern = when (packageName) {
            "com.google.android.apps.nbu.paisa.user" -> GOOGLE_PAY_MERCHANT
            "com.phonepe.app"                        -> PHONEPE_MERCHANT
            "net.one97.paytm"                        -> PAYTM_MERCHANT
            "com.hdfc.mobilebanking"                 -> HDFC_MERCHANT
            "com.csam.icici.bank.imobile"            -> ICICI_MERCHANT
            else                                     -> GENERIC_MERCHANT
        }
        val match = pattern.find(text)
        return match?.groupValues?.getOrNull(1)?.trim()
            ?.replace(Regex("""\s+"""), " ")
            ?.take(50)
            ?: ""
    }
}

