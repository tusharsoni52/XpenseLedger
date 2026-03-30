package com.xpenseledger.app.security.pin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class PinManager @Inject constructor(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "pin_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasPin(): Boolean = prefs.getString("hash", null) != null

    /**
     * Returns the length of the stored PIN, or 0 if none is set.
     * Used to detect legacy 4/5-digit PINs that must be migrated to 6 digits.
     */
    fun storedPinLength(): Int = prefs.getInt("pin_length", 0)

    fun savePin(pin: String) {
        val salt = UUID.randomUUID().toString()
        val hash = sha256(pin + salt)
        prefs.edit()
            .putString("hash", hash)
            .putString("salt", salt)
            .putInt("pin_length", pin.length)   // persist length for migration detection
            .apply()
    }

    fun validate(pin: String): Boolean {
        val salt   = prefs.getString("salt", "")!!
        val stored = prefs.getString("hash", "")!!
        return sha256(pin + salt) == stored
    }

    /** Wipes the stored PIN — called during the reset flow. */
    fun clearPin() {
        prefs.edit().remove("hash").remove("salt").remove("pin_length").apply()
    }

    private fun sha256(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}