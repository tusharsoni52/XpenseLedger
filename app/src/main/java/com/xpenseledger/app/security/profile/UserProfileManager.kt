package com.xpenseledger.app.security.profile

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject

/**
 * Stores user profile data (name, age, gender) in EncryptedSharedPreferences.
 * No PII is stored in plain SharedPreferences or Room.
 */
class UserProfileManager @Inject constructor(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "user_profile_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getName():   String = prefs.getString("name", "")   ?: ""
    fun getAge():    String = prefs.getString("age",  "")   ?: ""
    fun getGender(): String = prefs.getString("gender", "") ?: ""

    fun saveProfile(name: String, age: String, gender: String) {
        prefs.edit()
            .putString("name",   name.trim())
            .putString("age",    age.trim())
            .putString("gender", gender)
            .apply()
    }

    fun hasProfile(): Boolean = prefs.getString("name", null) != null

    fun clearProfile() {
        prefs.edit().clear().apply()
    }
}

