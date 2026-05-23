package com.xpenseledger.app.domain.usecase

import android.net.Uri
import android.content.ContentResolver
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.opencsv.CSVReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ImportFromExcelUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val contentResolver: ContentResolver,
) {
    suspend operator fun invoke(sourceUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                throw IllegalArgumentException("Cannot access the selected file. Please ensure the file exists and try selecting it again.")
            }

            inputStream.use { stream ->
                val reader = CSVReader(InputStreamReader(stream, Charsets.UTF_8))
                val allLines = try {
                    reader.readAll()
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid CSV file format. Please check the file and try again.")
                }

                if (allLines.isEmpty()) {
                    throw IllegalArgumentException("The selected file is empty. Please choose a valid CSV file.")
                }

                if (allLines.size < 2) {
                    throw IllegalArgumentException("No data found in the file. The file should contain at least a header row and one data row.")
                }

                // Skip header row
                val dataRows = allLines.drop(1)

                // Date and time formatters
                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

                var importedCount = 0
                var skippedCount = 0

                dataRows.forEach { row ->
                    try {
                        // Skip empty rows
                        if (row.size < 8 || row.getOrNull(1)?.isBlank() != false) {
                            skippedCount++
                            return@forEach
                        }

                        val title = row[1]
                        val amount = row[2].toDoubleOrNull()
                        if (amount == null) {
                            skippedCount++
                            return@forEach
                        }

                        val category = row[3]
                        val subCategory = row.getOrNull(4)?.takeIf { it.isNotBlank() }

                        // Parse date and time
                        val dateStr = row.getOrNull(5) ?: ""
                        val timeStr = row.getOrNull(6) ?: "00:00:00"
                        val timestamp = try {
                            val dateTime = "$dateStr $timeStr"
                            dateTimeFormat.parse(dateTime)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        // Parse transaction type
                        val typeStr = row.getOrNull(7) ?: "EXPENSE"
                        val type = try {
                            TransactionType.valueOf(typeStr.uppercase())
                        } catch (e: Exception) {
                            TransactionType.EXPENSE
                        }

                        val categoryId = row.getOrNull(8)?.toLongOrNull() ?: 0L
                        val subCategoryId = row.getOrNull(9)?.toLongOrNull()?.takeIf { it > 0 }

                        val expense = Expense(
                            id = 0L, // Let Room auto-generate new ID
                            title = title,
                            amount = amount,
                            category = category,
                            subCategory = subCategory,
                            categoryId = categoryId,
                            subCategoryId = subCategoryId,
                            timestamp = timestamp,
                            type = type
                        )

                        repository.insert(expense)
                        importedCount++
                    } catch (e: Exception) {
                        // Skip invalid rows
                        skippedCount++
                        e.printStackTrace()
                    }
                }

                reader.close()

                if (importedCount == 0) {
                    throw IllegalArgumentException("No valid transactions found in the file. Please check the file format.")
                }
            }
        } catch (e: SecurityException) {
            throw IllegalArgumentException("Permission denied. Please grant file access permission and try again.")
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalArgumentException("Failed to read file. Please ensure it's a valid CSV file and try again.")
        }
    }
}

