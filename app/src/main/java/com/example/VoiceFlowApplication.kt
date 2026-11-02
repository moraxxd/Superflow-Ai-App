package com.example

import android.app.Application
import com.example.ai.core.ModelManager
import com.example.data.VoiceFlowDatabase

class VoiceFlowApplication : Application() {

    companion object {
        lateinit var instance: VoiceFlowApplication
            private set
    }

    lateinit var database: VoiceFlowDatabase
        private set

    lateinit var modelManager: ModelManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = VoiceFlowDatabase.getDatabase(this)
        modelManager = ModelManager(this)
    }
}
