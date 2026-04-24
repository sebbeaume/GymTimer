package com.gymtimer

import android.app.Application
import com.gymtimer.data.UserPreferences
import com.gymtimer.data.db.GymDatabase

/**
 * The Application class is instantiated once when the process starts,
 * before any Activity or Service. It's the right place to initialise
 * app-wide singletons so every component can access them via
 * (application as GymTimerApp).database etc.
 */
class GymTimerApp : Application() {
    val database by lazy { GymDatabase.getDatabase(this) }
    val userPreferences by lazy { UserPreferences(this) }
}
