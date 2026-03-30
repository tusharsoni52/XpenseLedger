package com.xpenseledger.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xpenseledger.app.data.local.dao.CategoryDao
import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.entity.CategoryEntity
import com.xpenseledger.app.data.local.entity.ExpenseEntity

@Database(entities = [ExpenseEntity::class, CategoryEntity::class], version = 6)
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
         * v5 → v6 : Major category refactor + Fix orphaned transactions
         *
         * MERGED:
         *   - Mobile Recharge (34) → Mobile Bills (36)
         *   - Gas Cylinder (37) → Gas (32)
         *
         * MOVED:
         *   - Grocery (13): Food (1) → Household (9)
         *   - Insurance (53): Health (5) → Finance (7)
         *
         * NEW SUB-CATEGORIES:
         *   Transport: 25 Vehicle Service, 26 Parking
         *   Finance: 76 Vehicle Insurance, 77 Bank Charges
         *   Household: 92 Helper Salary, 93 House Maintenance
         *
         * NEW MAIN CATEGORY:
         *   200 Travel (MAIN)
         *   201-203: Travel subcategories
         *
         * CRITICAL FIX FOR MISSING TRANSACTIONS:
         *   - Find expenses with orphaned categoryId (not in categories table)
         *   - Remap them to categoryId = 82 (Miscellaneous) with fallback handling
         *   - Ensure "Other" and "Miscellaneous" categories exist
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ────────── ENSURE FALLBACK CATEGORIES EXIST ──────────────
                // Ensure "Other" MAIN category (id=8)
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (8, 'Other', 'MAIN', NULL, '📦')"
                )
                // Ensure "Miscellaneous" SUB category (id=82)
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (82, 'Miscellaneous', 'SUB', 8, '')"
                )

                // ────────── FIX ORPHANED EXPENSE REFERENCES ──────────────
                // Any expense with categoryId that doesn't exist in categories table
                // gets remapped to categoryId=82 (Miscellaneous)
                db.execSQL(
                    "UPDATE `expenses` SET `categoryId` = 82 " +
                    "WHERE `categoryId` NOT IN (SELECT `id` FROM `categories`) " +
                    "AND `categoryId` > 0"
                )

                // ────────── PRESERVE EXISTING CATEGORIES ──────────────
                // Do NOT delete any existing categories
                
                // ────────── MERGE DUPLICATES (if present) ──────────────
                // Mobile Recharge (34) → Mobile Bills (36)
                try {
                    db.execSQL(
                        "UPDATE `expenses` SET `categoryId` = 36 " +
                        "WHERE `categoryId` = 34 AND EXISTS (SELECT 1 FROM categories WHERE id = 34)"
                    )
                } catch (e: Exception) {
                    // Safe if doesn't exist
                }

                // Gas Cylinder (37) → Gas (32)
                try {
                    db.execSQL(
                        "UPDATE `expenses` SET `categoryId` = 32 " +
                        "WHERE `categoryId` = 37 AND EXISTS (SELECT 1 FROM categories WHERE id = 37)"
                    )
                } catch (e: Exception) {
                    // Safe if doesn't exist
                }

                // ────────── MOVE CATEGORIES (if they exist) ───────────────
                // Grocery (13): parentId 1 → 9
                try {
                    db.execSQL(
                        "UPDATE `categories` SET `parentId` = 9 " +
                        "WHERE `id` = 13"
                    )
                } catch (e: Exception) {
                    // Safe if doesn't exist
                }

                // Insurance (53): parentId 5 → 7
                try {
                    db.execSQL(
                        "UPDATE `categories` SET `parentId` = 7 " +
                        "WHERE `id` = 53"
                    )
                } catch (e: Exception) {
                    // Safe if doesn't exist
                }

                // ────────── ADD NEW TRANSPORT SUB-CATEGORIES ──────────────
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (25, 'Vehicle Service', 'SUB', 2, '')"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (26, 'Parking', 'SUB', 2, '')"
                )

                // ────────── ADD NEW FINANCE SUB-CATEGORIES ──────────────
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (76, 'Vehicle Insurance', 'SUB', 7, '')"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (77, 'Bank Charges', 'SUB', 7, '')"
                )

                // ────────── ADD NEW HOUSEHOLD SUB-CATEGORIES ──────────────
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (92, 'Helper Salary', 'SUB', 9, '')"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (93, 'House Maintenance', 'SUB', 9, '')"
                )

                // ────────── ADD NEW MAIN CATEGORY (Travel) ──────────────
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (200, 'Travel', 'MAIN', NULL, '✈️')"
                )

                // ────────── ADD TRAVEL SUB-CATEGORIES ──────────────
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (201, 'Flights', 'SUB', 200, '')"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (202, 'Hotels', 'SUB', 200, '')"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories` (`id`, `name`, `type`, `parentId`, `icon`) " +
                    "VALUES (203, 'Vacation', 'SUB', 200, '')"
                )
            }
        }
    }
}
