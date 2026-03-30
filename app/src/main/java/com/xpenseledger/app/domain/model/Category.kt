package com.xpenseledger.app.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: String,         // "MAIN" or "SUB"
    val parentId: Long? = null,
    val icon: String = ""
)
