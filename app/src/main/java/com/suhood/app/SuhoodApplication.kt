package com.suhood.app

import android.app.Application
import com.suhood.core.datastore.PreferencesRepository

class SuhoodApplication : Application() {
    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
    }
}
