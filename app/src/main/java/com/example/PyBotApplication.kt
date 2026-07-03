package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.PyBotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PyBotApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { 
        PyBotRepository(
            database.scriptDao(),
            database.moduleDao(),
            database.botConfigDao(),
            applicationScope
        )
    }
}
