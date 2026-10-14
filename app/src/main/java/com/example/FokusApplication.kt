package com.example

import android.app.Application
import com.example.data.database.FokusDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.FokusRepository

class FokusApplication : Application() {

    lateinit var database: FokusDatabase
        private set

    lateinit var repository: FokusRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = FokusDatabase.getInstance(this)
        repository = FokusRepository(database.fokusDao())
        preferencesManager = PreferencesManager(this)
    }

    companion object {
        lateinit var instance: FokusApplication
            private set
    }
}
