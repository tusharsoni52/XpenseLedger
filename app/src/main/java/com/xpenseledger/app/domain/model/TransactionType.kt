package com.xpenseledger.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Distinguishes how a transaction should be treated in analytics.
 *
 *  EXPENSE  — normal spending; counted in all expense totals and charts.
 *  INCOME   — money received; reserved for future use.
 *  TRANSFER — money sent to family / self; excluded from expense totals.
 *
 * Stored as its [name] string in the Room `expenses` table so no TypeConverter is needed.
 * Default for all existing rows (set via SQL DEFAULT) is [EXPENSE].
 */
@Serializable
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER;

    companion object {
        /** Safe valueOf — returns [EXPENSE] for any unknown/corrupt stored value. */
        fun fromString(value: String): TransactionType =
            entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}

