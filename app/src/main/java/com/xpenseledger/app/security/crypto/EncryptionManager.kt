package com.xpenseledger.app.security.crypto

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

private const val AES_MODE = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128

class EncryptionManager(private val keyStoreManager: KeyStoreManager) {

    fun encrypt(plain: ByteArray): ByteArray {
        val key = keyStoreManager.getOrCreateKey()
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        // prefix IV to ciphertext
        return iv + ciphertext
    }

    fun decrypt(data: ByteArray): ByteArray {
        val key = keyStoreManager.getOrCreateKey()
        val iv = data.copyOfRange(0, 12)
        val ciphertext = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }
}
