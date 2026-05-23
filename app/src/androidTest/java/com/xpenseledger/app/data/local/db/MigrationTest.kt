package com.xpenseledger.app.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Critical tests to prevent data loss during database migrations.
 * These tests ensure all migration paths work correctly without triggering
 * destructive fallback that would wipe user data.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create database with version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data in version 1 schema
            execSQL("INSERT INTO expenses VALUES (1, 'Test Expense', 100.0, 'Food', 1234567890)")
            close()
        }

        // Re-open database with version 2 and verify migration
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2).apply {
            // Verify data is preserved
            query("SELECT * FROM expenses WHERE id = 1").use { cursor ->
                assert(cursor.count == 1) { "Expense data was lost during migration 1→2" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO expenses VALUES (1, 'Test', 100.0, 'Food', NULL, 1, NULL, 1234567890)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3).apply {
            // Verify Home Rent category was added
            query("SELECT * FROM categories WHERE id = 35").use { cursor ->
                assert(cursor.count == 1) { "Home Rent category not added in migration 2→3" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO expenses VALUES (1, 'Test', 100.0, 'Food', NULL, 1, NULL, 1234567890)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4).apply {
            // Verify new categories were added
            query("SELECT COUNT(*) FROM categories WHERE id IN (9, 24, 36, 37, 74, 75, 90, 91)").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count == 8) { "Expected 8 new categories in migration 3→4, found $count" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO expenses VALUES (1, 'Test', 100.0, 'Food', NULL, 1, NULL, 1234567890)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5).apply {
            // Verify Grocery category was added
            query("SELECT * FROM categories WHERE id = 13 AND name = 'Grocery'").use { cursor ->
                assert(cursor.count == 1) { "Grocery category not added in migration 4→5" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        helper.createDatabase(TEST_DB, 5).apply {
            // Insert test expenses BEFORE migration
            execSQL("INSERT INTO expenses VALUES (1, 'Test Expense 1', 100.0, 'Food', NULL, 1, NULL, 1234567890)")
            execSQL("INSERT INTO expenses VALUES (2, 'Test Expense 2', 200.0, 'Transport', NULL, 2, NULL, 1234567891)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 6, true, AppDatabase.MIGRATION_5_6).apply {
            // CRITICAL: Verify expenses are preserved during category rebuild
            query("SELECT COUNT(*) FROM expenses").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count == 2) { "CRITICAL: Expenses were deleted during migration 5→6! Found $count, expected 2" }
            }

            // Verify new category structure exists
            query("SELECT COUNT(*) FROM categories").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count > 0) { "No categories found after migration 5→6" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        helper.createDatabase(TEST_DB, 6).apply {
            // Insert test expenses
            execSQL("INSERT INTO expenses VALUES (1, 'Test', 100.0, 'Food', NULL, 1, NULL, 1234567890)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 7, true, AppDatabase.MIGRATION_6_7).apply {
            // Verify type column was added with default value
            query("SELECT type FROM expenses WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                val type = cursor.getString(0)
                assert(type == "EXPENSE") { "Default type should be EXPENSE, found: $type" }
            }

            // Verify Family Support category was added
            query("SELECT * FROM categories WHERE id = 78").use { cursor ->
                assert(cursor.count == 1) { "Family Support category not added in migration 6→7" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL("INSERT INTO expenses VALUES (1, 'Test', 100.0, 'Food', NULL, 1, NULL, 1234567890, 'EXPENSE')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_7_8).apply {
            // Verify Income category and subcategories were added
            query("SELECT COUNT(*) FROM categories WHERE id IN (300, 301, 302, 303, 304, 305)").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count == 6) { "Expected 6 income categories in migration 7→8, found $count" }
            }
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL("INSERT INTO expenses VALUES (1, 'Test', 100.0, 'Food', NULL, 1, NULL, 1234567890, 'EXPENSE')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 9, true, AppDatabase.MIGRATION_8_9).apply {
            // Verify expenses are preserved (no-op migration)
            query("SELECT COUNT(*) FROM expenses").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count == 1) { "Expense was lost during no-op migration 8→9" }
            }
            close()
        }
    }

    /**
     * CRITICAL TEST: Full migration path from version 1 to latest
     * This simulates a user upgrading from the oldest version to current.
     * If this fails, users WILL LOSE DATA.
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll_1To9_preservesData() {
        // Start with version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert critical test data
            execSQL("INSERT INTO expenses VALUES (1, 'January Expense', 150.0, 'Food', 1609459200000)")
            execSQL("INSERT INTO expenses VALUES (2, 'February Expense', 250.0, 'Transport', 1612137600000)")
            execSQL("INSERT INTO expenses VALUES (3, 'March Expense', 350.0, 'Bills', 1614556800000)")
            close()
        }

        // Migrate through all versions to latest
        helper.runMigrationsAndValidate(
            TEST_DB, 9, true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        ).apply {
            // CRITICAL: Verify ALL expenses survived the migration chain
            query("SELECT COUNT(*) FROM expenses").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count == 3) {
                    "CRITICAL DATA LOSS: Expected 3 expenses after full migration 1→9, found $count"
                }
            }

            // Verify all expenses have the new type field
            query("SELECT COUNT(*) FROM expenses WHERE type = 'EXPENSE'").use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                assert(count == 3) { "Type field not properly defaulted during migration" }
            }

            close()
        }
    }
}

