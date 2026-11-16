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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val reason: String? = null,
    /** چهار رقم آخر کارت/حساب؛ null اگر در پیامک نبود. */
    val card: String? = null,
    /** مانده بعد از تراکنش (ریال) اگر در پیامک بود. */
    val balance: Long? = null
)

@Dao
interface TxDao {
    @Query("SELECT * FROM tx ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Tx>>

    @Query("SELECT * FROM tx")
    suspend fun all(): List<Tx>

    @Query("SELECT * FROM tx WHERE id = :id")
    suspend fun get(id: Long): Tx?

    @Query("SELECT reason FROM tx WHERE reason IS NOT NULL AND isIncome = :income ORDER BY timestamp DESC LIMIT 200")
    suspend fun recentReasons(income: Boolean): List<String>

    @Insert
    suspend fun insert(tx: Tx): Long

    @Insert
    suspend fun insertAll(list: List<Tx>)

    @Query("UPDATE tx SET reason = :reason WHERE id = :id")
    suspend fun setReason(id: Long, reason: String?)

    @Query("DELETE FROM tx WHERE sender = :sender")
    suspend fun removeBySender(sender: String)

    @Query("DELETE FROM tx WHERE id = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [Tx::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TxDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tx ADD COLUMN card TEXT")
                db.execSQL("ALTER TABLE tx ADD COLUMN balance INTEGER")
            }
        }

        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "yadkharj.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { inst = it }
        }
    }
}
