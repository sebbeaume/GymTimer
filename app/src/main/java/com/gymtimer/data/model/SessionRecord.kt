package com.gymtimer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room @Entity maps this Kotlin data class to a SQLite table called "sessions".
 * Each field becomes a column. @PrimaryKey(autoGenerate=true) means Room assigns
 * a unique ID automatically when we insert a new row.
 */
@Entity(tableName = "sessions")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTimestamp: Long,   // epoch millis — when the session began
    val durationSeconds: Long   // how long the session lasted
)
