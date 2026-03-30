package com.xpenseledger.app.security.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import android.util.Base64

class CryptoManager @Inject constructor(private val context: Context) {

    fun getDbKey(): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString("db_key", null)
        if (existing != null) return Base64.decode(existing, Base64.DEFAULT)

        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString("db_key", Base64.encodeToString(key, Base64.DEFAULT)).apply()
        return key
    }
}