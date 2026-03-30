package com.xpenseledger.app.domain.repository

import com.xpenseledger.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getMainCategories(): Flow<List<Category>>
    fun getSubCategories(parentId: Long): Flow<List<Category>>
    /**
     * Inserts every entry in [DefaultCategories] that does not yet exist in the DB.
     * Existing rows are left untouched (INSERT OR IGNORE semantics).
     * Safe to call on every app start — acts as a no-op for already-present rows.
     */
    suspend fun syncDefaults()
}
