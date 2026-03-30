package com.xpenseledger.app.di

import android.content.Context
import androidx.room.Room
import com.xpenseledger.app.data.local.db.AppDatabase
import com.xpenseledger.app.security.crypto.KeyStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphraseBytes = KeyStoreManager().getOrCreateKey().encoded
        val factory = SupportFactory(passphraseBytes)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xpenseledger_encrypted.db"
        )
            .openHelperFactory(factory)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideExpenseDao(db: AppDatabase) = db.expenseDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase) = db.categoryDao()
}
