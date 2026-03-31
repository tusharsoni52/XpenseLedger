package com.xpenseledger.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val category: String,           // main category name
    val subCategory: String? = null,
    val categoryId: Long = 0,
    val subCategoryId: Long? = null,
    val timestamp: Long,
    /** Defaults to EXPENSE so old serialized backups (missing this field) are safely restored. */
    val type: TransactionType = TransactionType.EXPENSE,
)
