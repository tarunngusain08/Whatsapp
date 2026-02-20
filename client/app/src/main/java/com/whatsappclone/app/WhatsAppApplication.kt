package com.whatsappclone.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.whatsappclone.core.common.util.UrlResolver
import com.whatsappclone.core.network.url.BaseUrlProvider
import com.whatsappclone.feature.chat.worker.ScheduledMessageWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class WhatsAppApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader = imageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        UrlResolver.init(BaseUrlProvider.DEFAULT_BASE_URL)
        createNotificationChannels()
        enqueueScheduledMessageWorker()
    }

    private fun enqueueScheduledMessageWorker() {
        val request = PeriodicWorkRequestBuilder<ScheduledMessageWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ScheduledMessageWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableVibration(true)
            }

            val groupsChannel = NotificationChannel(
                CHANNEL_GROUPS,
                "Group Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Group message notifications"
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
            }

            manager.createNotificationChannels(
                listOf(messagesChannel, groupsChannel, generalChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_GROUPS = "groups"
        const val CHANNEL_GENERAL = "general"
    }
}
