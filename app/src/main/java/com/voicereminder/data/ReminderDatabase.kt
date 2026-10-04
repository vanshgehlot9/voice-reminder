package com.voicereminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ============================================================
// Entity
// ============================================================

/**
 * Room entity representing a single reminder stored locally.
 *
 * scheduledAt is stored as Unix epoch milliseconds so AlarmManager
 * can consume it directly without any conversion.
 *
 * Low-end device note: Room uses SQLite which is bundled in Android —
 * zero extra disk footprint.
 */
@Entity(
    tableName = "reminders",
    indices = [Index("scheduledAt"), Index("isCompleted")]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,                    // UUID from the Android side (no server dependency)

    val task: String,                  // "Call John"
    val scheduledAt: Long,             // Unix epoch millis
    val scheduledAtIso: String,        // "2026-09-04T17:00:00+05:30" for display
    val timezone: String = "UTC",      // IANA timezone used when creating

    val transcript: String = "",       // Original STT text (useful for debugging)
    val replyText: String = "",        // TTS confirmation text

    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

// ============================================================
// DAO
// ============================================================

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    /** All pending reminders ordered by scheduled time — used for the list screen. */
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY scheduledAt ASC")
    fun observePending(): Flow<List<ReminderEntity>>

    /** All completed reminders. */
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY scheduledAt DESC")
    fun observeCompleted(): Flow<List<ReminderEntity>>

    /** All reminders (pending + completed) for history. */
    @Query("SELECT * FROM reminders ORDER BY scheduledAt DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    /** Reminders that have not yet fired but are in the past (missed). */
    @Query("""
        SELECT * FROM reminders
        WHERE isCompleted = 0 AND scheduledAt < :nowMillis
        ORDER BY scheduledAt ASC
    """)
    suspend fun getMissed(nowMillis: Long): List<ReminderEntity>

    /** Mark a reminder completed. */
    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)

    /** Used on boot to re-schedule all pending alarms. */
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND scheduledAt > :nowMillis")
    suspend fun getPendingAfter(nowMillis: Long): List<ReminderEntity>
}

// ============================================================
// Database
// ============================================================

@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "voicereminder.db"

        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getInstance(context: android.content.Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    DATABASE_NAME,
                )
                    // Allow destructive migration for MVP — add proper migrations before v2
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
