package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<KeywordRule>>

    @Query("SELECT * FROM keyword_rules WHERE id = :id")
    suspend fun getRuleById(id: Int): KeywordRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: KeywordRule)

    @Update
    suspend fun updateRule(rule: KeywordRule)

    @Delete
    suspend fun deleteRule(rule: KeywordRule)
}

@Dao
interface AlarmLogDao {
    @Query("SELECT * FROM alarm_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AlarmLog>>

    @Query("SELECT * FROM alarm_logs WHERE status = 'TRIGGERED' OR status = 'SNOOZED' ORDER BY timestamp DESC")
    fun getActiveAlarms(): Flow<List<AlarmLog>>

    @Query("SELECT * FROM alarm_logs WHERE id = :id")
    suspend fun getLogById(id: Int): AlarmLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AlarmLog): Long

    @Update
    suspend fun updateLog(log: AlarmLog)

    @Delete
    suspend fun deleteLog(log: AlarmLog)

    @Query("DELETE FROM alarm_logs")
    suspend fun clearAllLogs()
}

@Database(entities = [KeywordRule::class, AlarmLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun alarmLogDao(): AlarmLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_alarm_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
