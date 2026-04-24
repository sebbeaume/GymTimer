package com.gymtimer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gymtimer.data.model.SessionRecord

/**
 * @Database tells Room: "this is the database class, it owns these tables, version 1."
 * If we ever add columns or tables, we bump the version and provide a Migration.
 *
 * RoomDatabase is abstract — Room generates a concrete implementation at compile time.
 *
 * The companion object holds a singleton: we only ever want ONE database connection
 * open at a time. @Volatile ensures the instance is always read from main memory,
 * not a CPU cache, so all threads see the same value.
 */
@Database(entities = [SessionRecord::class], version = 1, exportSchema = false)
abstract class GymDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase {
            // Double-checked locking: avoids creating two instances in parallel
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
