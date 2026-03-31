package com.xpenseledger.app.data.local.dao

import androidx.room.*
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity)

    /** All transactions — used for full history and export. */
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    /** Only EXPENSE-type rows — used for totals and analytics charts. */
    @Query("SELECT * FROM expenses WHERE type != 'TRANSFER' ORDER BY timestamp DESC")
    fun getExpensesOnly(): Flow<List<ExpenseEntity>>

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}