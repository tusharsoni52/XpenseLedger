package com.xpenseledger.app.data.repository

import com.xpenseledger.app.data.local.dao.CategoryDao
import com.xpenseledger.app.data.local.entity.CategoryEntity
import com.xpenseledger.app.data.local.entity.DefaultCategories
import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        dao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getMainCategories(): Flow<List<Category>> =
        dao.getMainCategories().map { list -> list.map { it.toDomain() } }

    override fun getSubCategories(parentId: Long): Flow<List<Category>> =
        dao.getSubCategories(parentId).map { list -> list.map { it.toDomain() } }

    /**
     * Always inserts all default categories. The DAO uses OnConflictStrategy.IGNORE,
     * so existing rows are untouched while any new entries are added automatically.
     * This eliminates the need for a DB migration every time a new default category
     * is added to [DefaultCategories].
     */
    override suspend fun syncDefaults() {
        dao.insertAll(DefaultCategories.all)
    }

    private fun CategoryEntity.toDomain() =
        Category(id = id, name = name, type = type, parentId = parentId, icon = icon)
}
