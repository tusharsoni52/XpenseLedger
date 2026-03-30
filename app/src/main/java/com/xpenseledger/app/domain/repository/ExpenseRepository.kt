package com.xpenseledger.app.domain.repository

import com.xpenseledger.app.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun insert(expense: Expense)
    suspend fun update(expense: Expense)
    suspend fun delete(expense: Expense)
    fun getAll(): Flow<List<Expense>>
}