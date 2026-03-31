package com.xpenseledger.app.data.repository

import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : ExpenseRepository {

    override suspend fun insert(expense: Expense) = dao.insert(expense.toEntity())
    override suspend fun update(expense: Expense) = dao.update(expense.toEntity())
    override suspend fun delete(expense: Expense) = dao.delete(expense.toEntity())

    override fun getAll(): Flow<List<Expense>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    private fun Expense.toEntity() = ExpenseEntity(
        id            = id,
        title         = title,
        amount        = amount,
        category      = category,
        subCategory   = subCategory,
        categoryId    = categoryId,
        subCategoryId = subCategoryId,
        timestamp     = timestamp,
        type          = type.name          // store enum as string
    )

    private fun ExpenseEntity.toDomain() = Expense(
        id            = id,
        title         = title,
        amount        = amount,
        category      = category,
        subCategory   = subCategory,
        categoryId    = categoryId,
        subCategoryId = subCategoryId,
        timestamp     = timestamp,
        type          = TransactionType.fromString(type)  // safe parse, defaults to EXPENSE
    )
}
