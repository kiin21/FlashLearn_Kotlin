package com.kotlin.flashlearn

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class annotated with @HiltAndroidApp.
 * This triggers Hilt's code generation and serves as the application-level dependency container.
 * Implements Configuration.Provider for WorkManager Hilt integration.
 */
@HiltAndroidApp
class FlashlearnApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Initialize Cloudinary
        com.kotlin.flashlearn.data.remote.CloudinaryService.initialize(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
