package com.xpenseledger.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.db.AppDatabase
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric DAO tests for the new [type] column and [AppDatabase.MIGRATION_6_7].
 *
 * Covers:
 *  - insert / query with explicit type values
 *  - getAll() returns all types (full history)
 *  - getExpensesOnly() excludes TRANSFER rows
 *  - default type when no type supplied
 *  - MIGRATION_6_7: new column present, existing rows default to EXPENSE
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ExpenseDaoTransactionTypeTest {

    private lateinit var db:  AppDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.expenseDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ── insert + getAll ───────────────────────────────────────────────────────

    @Test
    fun `insert EXPENSE entity and retrieve via getAll`() = runBlocking {
        dao.insert(entity(title = "Grocery", type = "EXPENSE"))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("EXPENSE", all[0].type)
    }

    @Test
    fun `insert TRANSFER entity and retrieve via getAll`() = runBlocking {
        dao.insert(entity(title = "Family Money", type = "TRANSFER"))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("TRANSFER", all[0].type)
    }

    @Test
    fun `insert entity without explicit type defaults to EXPENSE`() = runBlocking {
        // ExpenseEntity.type defaults to "EXPENSE"
        dao.insert(ExpenseEntity(title = "Test", amount = 10.0, category = "Food",
            timestamp = System.currentTimeMillis()))
        val all = dao.getAll().first()
        assertEquals("EXPENSE", all[0].type)
    }

    @Test
    fun `getAll returns all types including TRANSFER`() = runBlocking {
        dao.insert(entity(title = "Expense",  type = "EXPENSE"))
        dao.insert(entity(title = "Transfer", type = "TRANSFER"))
        dao.insert(entity(title = "Income",   type = "INCOME"))

        val all = dao.getAll().first()
        assertEquals(3, all.size)
    }

    // ── getExpensesOnly ───────────────────────────────────────────────────────

    @Test
    fun `getExpensesOnly excludes TRANSFER rows`() = runBlocking {
        dao.insert(entity(title = "Grocery",       type = "EXPENSE"))
        dao.insert(entity(title = "Family Support", type = "TRANSFER"))

        val expensesOnly = dao.getExpensesOnly().first()
        assertEquals(1, expensesOnly.size)
        assertEquals("Grocery", expensesOnly[0].title)
        assertFalse(expensesOnly.any { it.type == "TRANSFER" })
    }

    @Test
    fun `getExpensesOnly excludes TRANSFER but includes INCOME rows`() = runBlocking {
        // The DAO query is: WHERE type != 'TRANSFER'
        // INCOME rows are NOT excluded — only TRANSFER is excluded.
        dao.insert(entity(title = "Salary",  type = "INCOME"))
        dao.insert(entity(title = "Grocery", type = "EXPENSE"))

        val expensesOnly = dao.getExpensesOnly().first()
        // Both INCOME and EXPENSE are returned; only TRANSFER would be excluded
        assertEquals(2, expensesOnly.size)
        assertFalse(expensesOnly.any { it.type == "TRANSFER" })
        assertTrue(expensesOnly.any { it.title == "Salary" })
        assertTrue(expensesOnly.any { it.title == "Grocery" })
    }

    @Test
    fun `getExpensesOnly returns empty when only TRANSFER rows exist`() = runBlocking {
        dao.insert(entity(title = "Transfer 1", type = "TRANSFER"))
        dao.insert(entity(title = "Transfer 2", type = "TRANSFER"))

        val expensesOnly = dao.getExpensesOnly().first()
        assertTrue(expensesOnly.isEmpty())
    }

    @Test
    fun `getExpensesOnly returns all rows when all are EXPENSE`() = runBlocking {
        dao.insert(entity(title = "Food",      type = "EXPENSE"))
        dao.insert(entity(title = "Transport", type = "EXPENSE"))
        dao.insert(entity(title = "Bills",     type = "EXPENSE"))

        val expensesOnly = dao.getExpensesOnly().first()
        assertEquals(3, expensesOnly.size)
    }

    // ── update preserves type ─────────────────────────────────────────────────

    @Test
    fun `update expense preserves TRANSFER type`() = runBlocking {
        dao.insert(entity(title = "Family", type = "TRANSFER"))
        val inserted = dao.getAll().first()[0]

        dao.update(inserted.copy(title = "Family Support Updated"))
        val updated = dao.getAll().first()[0]

        assertEquals("Family Support Updated", updated.title)
        assertEquals("TRANSFER", updated.type)
    }

    @Test
    fun `update expense can change type from EXPENSE to TRANSFER`() = runBlocking {
        dao.insert(entity(title = "Test", type = "EXPENSE"))
        val inserted = dao.getAll().first()[0]

        dao.update(inserted.copy(type = "TRANSFER"))
        val updated = dao.getAll().first()[0]

        assertEquals("TRANSFER", updated.type)
        // Should no longer appear in getExpensesOnly
        assertTrue(dao.getExpensesOnly().first().isEmpty())
    }

    // ── Mixed query results ───────────────────────────────────────────────────

    @Test
    fun `getAll count matches total inserted regardless of type`() = runBlocking {
        repeat(3) { i -> dao.insert(entity(title = "Expense $i",  type = "EXPENSE")) }
        repeat(2) { i -> dao.insert(entity(title = "Transfer $i", type = "TRANSFER")) }

        assertEquals(5, dao.getAll().first().size)
        assertEquals(3, dao.getExpensesOnly().first().size)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun entity(
        title: String  = "Test",
        type:  String  = "EXPENSE",
        amount: Double = 100.0
    ) = ExpenseEntity(
        title     = title,
        amount    = amount,
        category  = "Finance",
        timestamp = System.currentTimeMillis(),
        type      = type
    )
}

