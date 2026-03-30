package com.xpenseledger.app.di

import android.content.Context
import android.content.ContentResolver
import com.xpenseledger.app.security.crypto.EncryptionManager
import com.xpenseledger.app.security.crypto.KeyStoreManager
import com.xpenseledger.app.security.pin.PinManager
import com.xpenseledger.app.security.profile.UserProfileManager
import com.xpenseledger.app.security.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

	@Provides
	@Singleton
	fun providePinManager(@ApplicationContext context: Context): PinManager =
		PinManager(context)

	@Provides
	@Singleton
	fun provideSessionManager(): SessionManager = SessionManager()

	@Provides
	@Singleton
	fun provideKeyStoreManager(): KeyStoreManager = KeyStoreManager()

	@Provides
	@Singleton
	fun provideEncryptionManager(keyStoreManager: KeyStoreManager): EncryptionManager =
		EncryptionManager(keyStoreManager)

	@Provides
	@Singleton
	fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
		context.contentResolver

	@Provides
	@Singleton
	fun provideUserProfileManager(@ApplicationContext context: Context): UserProfileManager =
		UserProfileManager(context)
}

