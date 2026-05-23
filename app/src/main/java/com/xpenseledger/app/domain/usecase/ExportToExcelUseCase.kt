package com.xpenseledger.app.domain.usecase

import android.net.Uri
import android.content.ContentResolver
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.opencsv.CSVWriter
import kotlinx.coroutines.flow.first
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportToExcelUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val contentResolver: ContentResolver,
) {
    suspend operator fun invoke(targetUri: Uri) {
        val expenses = repository.getAll().first()

        // Date and time formatters
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

        contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            val writer = CSVWriter(OutputStreamWriter(outputStream))

            // Write header row
            writer.writeNext(arrayOf(
                "ID", "Title", "Amount", "Category", "Sub Category",
                "Date", "Time", "Type", "Category ID", "Sub Category ID"
            ))

            // Write data rows
            expenses.sortedByDescending { it.timestamp }.forEach { expense ->
                val date = Date(expense.timestamp)
                writer.writeNext(arrayOf(
                    expense.id.toString(),
                    expense.title,
                    expense.amount.toString(),
                    expense.category,
                    expense.subCategory ?: "",
                    dateFormat.format(date),
                    timeFormat.format(date),
                    expense.type.name,
                    expense.categoryId.toString(),
                    expense.subCategoryId?.toString() ?: ""
                ))
            }

            writer.close()
        }
    }
}

