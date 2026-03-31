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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ExpenseDaoRobolectricTest {
    private lateinit var db: AppDatabase
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

    @Test
    fun `insert and query expenses`() = runBlocking {
        val e = ExpenseEntity(title = "Test", amount = 10.0, category = "Food", timestamp = System.currentTimeMillis())
        dao.insert(e)

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Test", all[0].title)
    }
}
