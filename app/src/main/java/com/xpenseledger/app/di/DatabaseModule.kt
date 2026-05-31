package com.xpenseledger.app.di

import android.content.Context
import androidx.room.Room
import com.xpenseledger.app.data.local.db.AppDatabase
import com.xpenseledger.app.data.local.db.AutoBackupCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xpenseledger.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10
            )
            // Add automatic backup callback to prevent data loss
            .addCallback(AutoBackupCallback(context))
            // REMOVED: .fallbackToDestructiveMigration()
            // This was causing SILENT DATA LOSS on migration failures.
            // Better to crash with clear error than lose user's expense data.
            // If migration fails, user will see error and can restore from backup.
            .build()
    }

    @Provides
    fun provideExpenseDao(db: AppDatabase) = db.expenseDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase) = db.categoryDao()

    @Provides
    fun providePendingTransactionDao(db: AppDatabase) = db.pendingTransactionDao()
}
