package com.xpenseledger.app

import com.xpenseledger.app.data.local.entity.ExpenseEntity
import com.xpenseledger.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ExpenseEntity] Room entity with the new [type] column.
 * Covers:
 *  - default type value ("EXPENSE")
 *  - correct string storage (enum name)
 *  - mapper logic mirrored from ExpenseRepositoryImpl
 */
class ExpenseEntityTest {

    private val baseEntity = ExpenseEntity(
        title     = "Groceries",
        amount    = 200.0,
        category  = "Household",
        timestamp = 1_000_000L
    )

    // ── Default value ─────────────────────────────────────────────────────────

    @Test
    fun `ExpenseEntity defaults type to EXPENSE string`() {
        assertEquals("EXPENSE", baseEntity.type)
    }

    @Test
    fun `ExpenseEntity can store TRANSFER string`() {
        val entity = baseEntity.copy(type = "TRANSFER")
        assertEquals("TRANSFER", entity.type)
    }

    @Test
    fun `ExpenseEntity can store INCOME string`() {
        val entity = baseEntity.copy(type = "INCOME")
        assertEquals("INCOME", entity.type)
    }

    // ── Mapper: Expense → ExpenseEntity ──────────────────────────────────────

    @Test
    fun `EXPENSE domain type name equals EXPENSE`() {
        val typeString = TransactionType.EXPENSE.name
        assertEquals("EXPENSE", typeString)
    }

    @Test
    fun `TRANSFER domain type name equals TRANSFER`() {
        val typeString = TransactionType.TRANSFER.name
        assertEquals("TRANSFER", typeString)
    }

    // ── Mapper: ExpenseEntity → Expense (fromString) ──────────────────────────

    @Test
    fun `entity EXPENSE string maps back to EXPENSE enum`() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString(baseEntity.type))
    }

    @Test
    fun `entity TRANSFER string maps back to TRANSFER enum`() {
        val entity = baseEntity.copy(type = "TRANSFER")
        assertEquals(TransactionType.TRANSFER, TransactionType.fromString(entity.type))
    }

    @Test
    fun `corrupt entity type string maps safely to EXPENSE`() {
        val entity = baseEntity.copy(type = "CORRUPT_VALUE")
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString(entity.type))
    }
}
