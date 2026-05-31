package com.xpenseledger.app.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val status: String = "PENDING"
)