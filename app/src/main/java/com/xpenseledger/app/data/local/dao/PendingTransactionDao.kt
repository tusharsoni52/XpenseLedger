package com.xpenseledger.app.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xpenseledger.app.data.local.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingTransactionEntity): Long
    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY detectedAt DESC")
    fun getPending(): Flow<List<PendingTransactionEntity>>
    @Query("SELECT * FROM pending_transactions ORDER BY detectedAt DESC")
    suspend fun getPendingSnapshot(): List<PendingTransactionEntity>
    @Query("SELECT COUNT(*) FROM pending_transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    @Query("SELECT EXISTS(SELECT 1 FROM pending_transactions WHERE dedupeHash = :hash AND detectedAt > :afterMillis)")
    suspend fun existsByHash(hash: String, afterMillis: Long): Boolean
    @Query("DELETE FROM pending_transactions WHERE status != 'PENDING' AND detectedAt < :beforeMillis")
    suspend fun deleteOldNonPending(beforeMillis: Long)
}