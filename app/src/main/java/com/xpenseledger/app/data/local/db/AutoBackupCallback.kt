package com.xpenseledger.app.data.local.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Automatic backup callback that creates database backups before migrations.
 * Prevents permanent data loss by maintaining backup copies.
 */
class AutoBackupCallback(private val context: Context) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Create backup on database open (before any migration)
        CoroutineScope(Dispatchers.IO).launch {
            createAutomaticBackup()
        }
    }

    private fun createAutomaticBackup() {
        try {
            val dbFile = context.getDatabasePath("xpenseledger.db")
            if (!dbFile.exists()) return

            // Create backups directory
            val backupDir = File(context.filesDir, "db_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Create timestamped backup file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "xpenseledger_auto_backup_$timestamp.db")

            // Copy database file
            dbFile.copyTo(backupFile, overwrite = false)

            // Keep only last 5 automatic backups to save space
            cleanOldBackups(backupDir)

            android.util.Log.i("AutoBackup", "Database backup created: ${backupFile.name}")
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "Failed to create automatic backup", e)
        }
    }

    private fun cleanOldBackups(backupDir: File) {
        try {
            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith("xpenseledger_auto_backup_") && file.extension == "db"
            }?.sortedByDescending { it.lastModified() } ?: return

            // Keep only the 5 most recent backups
            backupFiles.drop(5).forEach { file ->
                file.delete()
                android.util.Log.i("AutoBackup", "Deleted old backup: ${file.name}")
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "Failed to clean old backups", e)
        }
    }
}

