package com.xpenseledger.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,           // main category name
    val subCategory: String? = null,
    val categoryId: Long = 0,
    val subCategoryId: Long? = null,
    val timestamp: Long,
    /** Stored as enum name string; DEFAULT 'EXPENSE' set via MIGRATION_6_7. */
    @ColumnInfo(name = "type", defaultValue = "EXPENSE")
    val type: String = "EXPENSE"
)
