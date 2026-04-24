package com.gymtimer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gymtimer.data.model.SessionRecord
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object.
 * Room reads these annotated functions and generates the actual SQL at compile time.
 *
 * Flow<List<SessionRecord>> means the UI will automatically re-render
 * whenever the database changes — no manual refresh needed.
 */
@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionRecord)

    /**
     * Returns all sessions newest-first.
     * "suspend" = runs on a background coroutine (won't block the UI thread).
     * Flow = reactive stream: every DB change re-emits automatically.
     */
    @Query("SELECT * FROM sessions ORDER BY startTimestamp DESC")
    fun getAllSessions(): Flow<List<SessionRecord>>

    @Delete
    suspend fun delete(session: SessionRecord)
}
