package com.xpenseledger.app.domain.usecase

import android.net.Uri
import android.content.ContentResolver
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.xpenseledger.app.security.crypto.EncryptionManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ImportExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val encryptionManager: EncryptionManager,
    private val contentResolver: ContentResolver,
) {
    suspend operator fun invoke(sourceUri: Uri) {
        val bytes = contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return
        val decrypted = encryptionManager.decrypt(bytes)
        val expenses = Json.decodeFromString<List<Expense>>(String(decrypted))
        expenses.forEach { repository.insert(it) }
    }
}
