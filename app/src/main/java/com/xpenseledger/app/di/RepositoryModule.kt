package com.xpenseledger.app.di

import com.xpenseledger.app.data.repository.CategoryRepositoryImpl
import com.xpenseledger.app.data.repository.ExpenseRepositoryImpl
import com.xpenseledger.app.domain.repository.CategoryRepository
import com.xpenseledger.app.domain.repository.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindExpenseRepo(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    abstract fun bindCategoryRepo(impl: CategoryRepositoryImpl): CategoryRepository
}
