package com.xpenseledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String,         // "MAIN" or "SUB"
    val parentId: Long? = null,
    val icon: String = ""
)
