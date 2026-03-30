package com.xpenseledger.app.domain.usecase

import android.net.Uri
import android.content.ContentResolver
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.xpenseledger.app.security.crypto.EncryptionManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ExportExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val encryptionManager: EncryptionManager,
    private val contentResolver: ContentResolver,
) {
    suspend operator fun invoke(targetUri: Uri) {
        val expenses = repository.getAll().first()
        val json = Json.encodeToString(expenses)
        val encrypted = encryptionManager.encrypt(json.toByteArray())
        contentResolver.openOutputStream(targetUri)?.use { it.write(encrypted) }
    }
}
