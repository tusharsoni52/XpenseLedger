package com.xpenseledger.app

import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import com.xpenseledger.app.data.repository.ExpenseRepositoryImpl
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ExpenseRepositoryImpl] mapper round-trips with [TransactionType].
 */
class ExpenseRepositoryMapperTest {

    private val dao  = mockk<ExpenseDao>(relaxed = true)
    private val repo = ExpenseRepositoryImpl(dao)

    private val baseExpense = Expense(
        id        = 1L,
        title     = "Test",
        amount    = 100.0,
        category  = "Finance",
        timestamp = 1_000_000L,
        type      = TransactionType.EXPENSE
    )

    private val baseEntity = ExpenseEntity(
        id        = 1L,
        title     = "Test",
        amount    = 100.0,
        category  = "Finance",
        timestamp = 1_000_000L,
        type      = "EXPENSE"
    )

    // ── Expense → ExpenseEntity (insert) ─────────────────────────────────────

    @Test
    fun `insert stores EXPENSE type as EXPENSE name string`() = runTest {
        val slot = slot<ExpenseEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit
        repo.insert(baseExpense)
        assertEquals("EXPENSE", slot.captured.type)
    }

    @Test
    fun `insert stores TRANSFER type as TRANSFER name string`() = runTest {
        val slot = slot<ExpenseEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit
        repo.insert(baseExpense.copy(type = TransactionType.TRANSFER))
        assertEquals("TRANSFER", slot.captured.type)
    }

    @Test
    fun `insert stores INCOME type as INCOME name string`() = runTest {
        val slot = slot<ExpenseEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit
        repo.insert(baseExpense.copy(type = TransactionType.INCOME))
        assertEquals("INCOME", slot.captured.type)
    }

    // ── Expense → ExpenseEntity (update) ─────────────────────────────────────

    @Test
    fun `update stores TRANSFER type as TRANSFER name string`() = runTest {
        val slot = slot<ExpenseEntity>()
        coEvery { dao.update(capture(slot)) } returns Unit
        repo.update(baseExpense.copy(type = TransactionType.TRANSFER))
        assertEquals("TRANSFER", slot.captured.type)
    }

    // ── ExpenseEntity → Expense (getAll) ─────────────────────────────────────

    @Test
    fun `getAll maps EXPENSE string to EXPENSE enum`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(baseEntity.copy(type = "EXPENSE")))
        val result = repo.getAll().first()
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `getAll maps TRANSFER string to TRANSFER enum`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(baseEntity.copy(type = "TRANSFER")))
        val result = repo.getAll().first()
        assertEquals(TransactionType.TRANSFER, result[0].type)
    }

    @Test
    fun `getAll maps INCOME string to INCOME enum`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(baseEntity.copy(type = "INCOME")))
        val result = repo.getAll().first()
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    @Test
    fun `getAll maps corrupt type string to EXPENSE as safe default`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(baseEntity.copy(type = "CORRUPTED")))
        val result = repo.getAll().first()
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `getAll maps mixed types correctly`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(
            baseEntity.copy(id = 1L, type = "EXPENSE"),
            baseEntity.copy(id = 2L, type = "TRANSFER"),
            baseEntity.copy(id = 3L, type = "INCOME")
        ))
        val result = repo.getAll().first()
        assertEquals(TransactionType.EXPENSE,  result[0].type)
        assertEquals(TransactionType.TRANSFER, result[1].type)
        assertEquals(TransactionType.INCOME,   result[2].type)
    }

    // ── Round-trip ────────────────────────────────────────────────────────────

    @Test
    fun `round-trip TRANSFER — insert then retrieve preserves type`() = runTest {
        val transferExpense = baseExpense.copy(type = TransactionType.TRANSFER)
        val insertSlot = slot<ExpenseEntity>()
        coEvery { dao.insert(capture(insertSlot)) } returns Unit
        repo.insert(transferExpense)
        every { dao.getAll() } returns flowOf(listOf(insertSlot.captured))
        val result = repo.getAll().first()
        assertEquals(TransactionType.TRANSFER, result[0].type)
    }
}
