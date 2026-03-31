package com.xpenseledger.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xpenseledger.app.data.local.dao.CategoryDao
import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.entity.CategoryEntity
import com.xpenseledger.app.data.local.entity.ExpenseEntity

@Database(entities = [ExpenseEntity::class, CategoryEntity::class], version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `parentId` INTEGER,
                        `icon` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `subCategory` TEXT")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `categoryId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `subCategoryId` INTEGER")
            }
        }

        /** Adds Home Rent sub-category under Bills (id=35, parentId=3). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (35, 'Home Rent', 'SUB', 3, '')"
                )
            }
        }

        /**
         * v3 → v4 : New categories batch
         *
         * New MAIN:
         *   9  Household 🏠
         *
         * New SUBs:
         *   Transport : 24 Road Toll
         *   Bills     : 36 Mobile Bills, 37 Gas Cylinder
         *   Finance   : 74 Recurring Deposit, 75 Fixed Deposit
         *   Household : 90 Maid Salary, 91 Cook Salary
         *
         * Credit Card Payment (id=71) already exists — not re-inserted.
         * All statements use INSERT OR IGNORE for safe re-runs / rollback resilience.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val ins = "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES"

                // New MAIN category
                db.execSQL("$ins (9,  'Household',         'MAIN', NULL, '🏠')")

                // Transport subcategories
                db.execSQL("$ins (24, 'Road Toll',         'SUB',  2,    '')")

                // Bills subcategories
                db.execSQL("$ins (36, 'Mobile Bills',      'SUB',  3,    '')")
                db.execSQL("$ins (37, 'Gas Cylinder',      'SUB',  3,    '')")

                // Finance subcategories
                db.execSQL("$ins (74, 'Recurring Deposit', 'SUB',  7,    '')")
                db.execSQL("$ins (75, 'Fixed Deposit',     'SUB',  7,    '')")

                // Household subcategories
                db.execSQL("$ins (90, 'Maid Salary',       'SUB',  9,    '')")
                db.execSQL("$ins (91, 'Cook Salary',       'SUB',  9,    '')")
            }
        }

        /** v4 → v5 : Adds Grocery sub-category under Food (id=13, parentId=1). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (13, 'Grocery', 'SUB', 1, '')"
                )
            }
        }

        /**
         * v5 → v6 : Complete category refactor with fresh start
         *
         * STRATEGY: Clear old expenses to avoid compatibility issues
         * RESULT: Clean slate with new category structure
         *
         * NEW MAIN CATEGORIES (All compatible with new structure):
         *   1 Food, 2 Transport, 3 Bills, 4 Shopping, 5 Health
         *   6 Entertainment, 7 Finance, 8 Other, 9 Household, 200 Travel
         *
         * All subcategories defined in DefaultCategories.kt
         * Users will add fresh expenses with new categories
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ────────── CLEAR OLD EXPENSES ──────────────────────────
                // Delete all old expenses that may have incompatible category references
                db.execSQL("DELETE FROM `expenses`")

                // ────────── CLEAR OLD CATEGORIES ──────────────────────────
                db.execSQL("DELETE FROM `categories`")

                // ────────── INSERT NEW CATEGORIES (From DefaultCategories) ──────────────────
                // MAIN Categories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (1, 'Food', 'MAIN', NULL, '🍽')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (2, 'Transport', 'MAIN', NULL, '🚗')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (3, 'Bills', 'MAIN', NULL, '📋')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (4, 'Shopping', 'MAIN', NULL, '🛒')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (5, 'Health', 'MAIN', NULL, '💊')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (6, 'Entertainment', 'MAIN', NULL, '🎬')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (7, 'Finance', 'MAIN', NULL, '💰')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (8, 'Other', 'MAIN', NULL, '📦')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (9, 'Household', 'MAIN', NULL, '🏠')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (200, 'Travel', 'MAIN', NULL, '✈️')")

                // Food subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (10, 'Dining Out', 'SUB', 1, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (11, 'Coffee & Snacks', 'SUB', 1, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (12, 'Food Delivery', 'SUB', 1, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (13, 'Grocery', 'SUB', 9, '')")

                // Transport subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (20, 'Fuel', 'SUB', 2, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (21, 'Cab / Auto', 'SUB', 2, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (22, 'Public Transport', 'SUB', 2, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (23, 'Travel', 'SUB', 2, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (24, 'Road Toll', 'SUB', 2, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (25, 'Vehicle Service', 'SUB', 2, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (26, 'Parking', 'SUB', 2, '')")

                // Bills subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (30, 'Electricity', 'SUB', 3, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (31, 'Water', 'SUB', 3, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (32, 'Gas', 'SUB', 3, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (33, 'Internet', 'SUB', 3, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (35, 'Home Rent', 'SUB', 3, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (36, 'Mobile Bills', 'SUB', 3, '')")

                // Shopping subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (40, 'Clothing', 'SUB', 4, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (41, 'Electronics', 'SUB', 4, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (42, 'Personal Care', 'SUB', 4, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (43, 'Misc Shopping', 'SUB', 4, '')")

                // Health subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (50, 'Doctor', 'SUB', 5, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (51, 'Medicines', 'SUB', 5, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (52, 'Gym / Fitness', 'SUB', 5, '')")

                // Entertainment subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (60, 'OTT Subscriptions', 'SUB', 6, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (61, 'Movies / Events', 'SUB', 6, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (62, 'Games', 'SUB', 6, '')")

                // Finance subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (70, 'EMI / Loans', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (71, 'Credit Card Payment', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (72, 'Taxes', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (73, 'Investments', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (74, 'Recurring Deposit', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (75, 'Fixed Deposit', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (53, 'Insurance', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (76, 'Vehicle Insurance', 'SUB', 7, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (77, 'Bank Charges', 'SUB', 7, '')")

                // Household subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (90, 'Maid Salary', 'SUB', 9, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (91, 'Cook Salary', 'SUB', 9, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (92, 'Helper Salary', 'SUB', 9, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (93, 'House Maintenance', 'SUB', 9, '')")

                // Travel subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (201, 'Flights', 'SUB', 200, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (202, 'Hotels', 'SUB', 200, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (203, 'Vacation', 'SUB', 200, '')")

                // Other subcategories
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (80, 'Gifts', 'SUB', 8, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (81, 'Donations', 'SUB', 8, '')")
                db.execSQL("INSERT INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES (82, 'Miscellaneous', 'SUB', 8, '')")
            }
        }

        /**
         * v6 → v7 : Adds TransactionType support + Family Support sub-category
         *
         * Changes:
         *  1. Add `type` column to `expenses` (TEXT NOT NULL DEFAULT 'EXPENSE')
         *     — all existing rows are automatically set to EXPENSE (no data loss)
         *  2. Insert "Family Support" sub-category under Finance (id=7)
         *
         * NOTE: Keyword-based auto-tagging is intentionally skipped to avoid
         *       unexpected changes to existing users' expense totals.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (78, 'Family Support', 'SUB', 7, '👨\u200D👩\u200D👧')"
                )
            }
        }

        /** v7 → v8 : Adds Income main category and subcategories. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val ins = "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) VALUES"
                db.execSQL("$ins (300, 'Income',       'MAIN', NULL, '💵')")
                db.execSQL("$ins (301, 'Salary',       'SUB',  300,  '')")
                db.execSQL("$ins (302, 'Bonus',        'SUB',  300,  '')")
                db.execSQL("$ins (303, 'Freelance',    'SUB',  300,  '')")
                db.execSQL("$ins (304, 'Interest',     'SUB',  300,  '')")
                db.execSQL("$ins (305, 'Other Income', 'SUB',  300,  '')")
            }
        }
    }
}
