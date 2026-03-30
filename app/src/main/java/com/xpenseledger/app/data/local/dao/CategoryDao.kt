package com.xpenseledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xpenseledger.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /** All categories ordered: MAIN first, then SUB, each group by id */
    @Query("SELECT * FROM categories ORDER BY CASE type WHEN 'MAIN' THEN 0 ELSE 1 END, id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = 'MAIN' ORDER BY id ASC")
    fun getMainCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = 'SUB' AND parentId = :parentId ORDER BY id ASC")
    fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    // ─ DEBUG METHODS ─────────────────────────────────────────────────────────

    /**
     * DEBUG: Returns fallback category IDs that exist.
     * Used to verify migration 5→6 created fallback categories.
     */
    @Query("SELECT id FROM categories WHERE id IN (8, 82)")
    suspend fun debugGetFallbackCategories(): List<Long>
}
