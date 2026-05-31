package com.xpenseledger.app.notification.data

import com.xpenseledger.app.data.local.dao.PendingTransactionDao
import com.xpenseledger.app.data.local.entity.PendingTransactionEntity
import com.xpenseledger.app.notification.model.PendingTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingTransactionRepositoryImpl @Inject constructor(
    private val dao: PendingTransactionDao
) {
    fun getPending(): Flow<List<PendingTransaction>> =
        dao.getPending().map { list -> list.map { it.toDomain() } }

    fun getPendingCount(): Flow<Int> = dao.getPendingCount()

    suspend fun insert(tx: PendingTransaction): Long = dao.insert(tx.toEntity())

    suspend fun markAdded(id: Long)     = dao.updateStatus(id, PendingTransaction.STATUS_ADDED)
    suspend fun markDismissed(id: Long) = dao.updateStatus(id, PendingTransaction.STATUS_DISMISSED)

    suspend fun isDuplicate(hash: String, withinMillis: Long = 10_000L): Boolean =
        dao.existsByHash(hash, System.currentTimeMillis() - withinMillis)

    suspend fun cleanupOld() =
        dao.deleteOldNonPending(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)

    private fun PendingTransaction.toEntity() = PendingTransactionEntity(
        id              = id,
        sourcePackage   = sourcePackage,
        sourceLabel     = sourceLabel,
        amount          = amount,
        merchant        = merchant,
        category        = category,
        subCategory     = subCategory,
        categoryId      = categoryId,
        subCategoryId   = subCategoryId,
        transactionType = transactionType,
        rawText         = rawText,
        detectedAt      = detectedAt,
        dedupeHash      = dedupeHash,
        status          = status
    )

    private fun PendingTransactionEntity.toDomain() = PendingTransaction(
        id              = id,
        sourcePackage   = sourcePackage,
        sourceLabel     = sourceLabel,
        amount          = amount,
        merchant        = merchant,
        category        = category,
        subCategory     = subCategory,
        categoryId      = categoryId,
        subCategoryId   = subCategoryId,
        transactionType = transactionType,
        rawText         = rawText,
        detectedAt      = detectedAt,
        dedupeHash      = dedupeHash,
        status          = status
    )
}
