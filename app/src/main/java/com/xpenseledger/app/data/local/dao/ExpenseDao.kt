package com.xpenseledger.app.data.local.dao

import androidx.room.*
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

// ─── Helper data classes for debug queries ───────────────────────────────────
data class CategoryReferenceCount(
    @ColumnInfo(name = "categoryId") val categoryId: Long,
    @ColumnInfo(name = "count") val count: Long
)

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    // ─ DEBUG METHODS ─────────────────────────────────────────────────────────
    
    /**
     * DEBUG: Count total expenses in database.
     * If this returns > 0 → data exists (not deleted)
     */
    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun debugCountAllExpenses(): Long

    /**
     * DEBUG: Get all expenses with no filters.
     * Use this to check if the issue is in filtering logic.
     */
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun debugGetAllExpensesNoFilter(): List<ExpenseEntity>

    /**
     * DEBUG: Count expenses with valid category references.
     * Compare with debugCountAllExpenses() to find orphaned records.
     */
    @Query(
        """
        SELECT COUNT(*) FROM expenses e
        WHERE e.categoryId IN (SELECT id FROM categories)
        """
    )
    suspend fun debugCountExpensesWithValidCategories(): Long

    /**
     * DEBUG: Get all expenses with ORPHANED categoryId references.
     * These should be remapped to fallback category (82) by migration 5→6.
     */
    @Query(
        """
        SELECT * FROM expenses e
        WHERE e.categoryId NOT IN (SELECT id FROM categories)
        AND e.categoryId > 0
        ORDER BY e.timestamp DESC
        """
    )
    suspend fun debugGetOrphanedExpenses(): List<ExpenseEntity>

    /**
     * DEBUG: Count how many expenses reference each category.
     * Returns (categoryId, count) to find missing categories.
     */
    @Query(
        """
        SELECT categoryId, COUNT(*) as count FROM expenses
        WHERE categoryId > 0
        GROUP BY categoryId
        ORDER BY count DESC
        """
    )
    suspend fun debugCategoryReferenceCount(): List<CategoryReferenceCount>

    /**
     * DEBUG: Verify category integrity after migration.
     * Returns categoryIds that have expenses but no matching category record.
     */
    @Query(
        """
        SELECT DISTINCT e.categoryId FROM expenses e
        WHERE e.categoryId NOT IN (SELECT id FROM categories)
        AND e.categoryId > 0
        """
    )
    suspend fun debugGetMissingCategoryIds(): List<Long>

    /**
     * DEBUG: Get all expenses for a specific date range (helpful for month selection).
     */
    @Query(
        """
        SELECT * FROM expenses
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        ORDER BY timestamp DESC
        """
    )
    suspend fun debugGetExpensesByDateRange(startMillis: Long, endMillis: Long): List<ExpenseEntity>
}