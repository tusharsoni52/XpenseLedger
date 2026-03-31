package com.xpenseledger.app.security.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import android.util.Base64

class CryptoManager @Inject constructor(private val context: Context) {

    private var cachedDbKey: ByteArray? = null
    private var cachedMasterKey: MasterKey? = null
    private var cachedPrefs: SharedPreferences? = null

    /**
     * Get or create the database encryption key with caching to avoid
     * expensive MasterKey creation and SharedPreferences initialization.
     * Keys are cached for the lifetime of this singleton instance.
     */
    fun getDbKey(): ByteArray {
        return cachedDbKey ?: run {
            // Initialize MasterKey once and cache it
            if (cachedMasterKey == null) {
                cachedMasterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            }

            // Initialize encrypted prefs once and cache it
            if (cachedPrefs == null) {
                cachedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    cachedMasterKey!!,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }

            val existing = cachedPrefs!!.getString("db_key", null)
            if (existing != null) {
                Base64.decode(existing, Base64.DEFAULT)
            } else {
                ByteArray(32).also {
                    SecureRandom().nextBytes(it)
                    cachedPrefs!!.edit()
                        .putString("db_key", Base64.encodeToString(it, Base64.DEFAULT))
                        .apply()
                }
            }
        }.also { cachedDbKey = it }
    }
}