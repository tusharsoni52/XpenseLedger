package com.xpenseledger.app.notification.model

/**
 * Domain model for a transaction detected from a payment notification
 * that is awaiting user confirmation.
 */
data class PendingTransaction(
    val id: Long = 0L,
    val sourcePackage: String,
    val sourceLabel: String,
    val amount: Double,
    val merchant: String,
    val category: String,
    val subCategory: String,
    val categoryId: Long,
    val subCategoryId: Long?,
    val transactionType: String,
    val rawText: String,
    val detectedAt: Long,
    val dedupeHash: String,
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING   = "PENDING"
        const val STATUS_ADDED     = "ADDED"
        const val STATUS_DISMISSED = "DISMISSED"
    }
}

