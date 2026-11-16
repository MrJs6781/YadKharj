package com.yadkharj.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** amount همیشه به «ریال» ذخیره می‌شود. reason == null یعنی هنوز دلیلش ثبت نشده. */
@Entity(tableName = "tx", indices = [Index("timestamp")])
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val isIncome: Boolean,
    val timestamp: Long,
    val sender: String,
    val body: String,
    val reason: String? = null
)

@Dao
interface TxDao {
    @Query("SELECT * FROM tx ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Tx>>

    @Query("SELECT * FROM tx WHERE id = :id")
    suspend fun get(id: Long): Tx?

    @Query("SELECT reason FROM tx WHERE reason IS NOT NULL AND isIncome = :income ORDER BY timestamp DESC LIMIT 200")
    suspend fun recentReasons(income: Boolean): List<String>

    @Insert
    suspend fun insert(tx: Tx): Long

    @Query("UPDATE tx SET reason = :reason WHERE id = :id")
    suspend fun setReason(id: Long, reason: String?)

    @Query("DELETE FROM tx WHERE id = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [Tx::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TxDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "yadkharj.db")
                .build().also { inst = it }
        }
    }
}
