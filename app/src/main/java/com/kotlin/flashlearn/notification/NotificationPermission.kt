package com.kotlin.flashlearn.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kotlin.flashlearn.notification.daily.DailyReminderScheduler
import com.kotlin.flashlearn.presentation.noti.AutoReminderPrefs
import com.kotlin.flashlearn.notification.exam.ExamReminderScheduler
import com.kotlin.flashlearn.presentation.noti.ExamReminderPrefs

/**
 * Call this once in MainActivity.onCreate().
 * It will request POST_NOTIFICATIONS on Android 13+ if needed.
 * When granted, it auto-schedules daily reminder exactly once.
 */
class DailyReminderPermissionGate(
    private val activity: ComponentActivity
) {

    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onGranted?.invoke()
        }

    private var onGranted: (() -> Unit)? = null

    fun ensureDailyReminder(
        hour: Int,
        minute: Int,
        title: String,
        body: String
    ) {
        val ctx = activity.applicationContext

        fun scheduleOnce() {
            if (AutoReminderPrefs.wasScheduled(ctx)) return
            DailyReminderScheduler.schedule(ctx, hour, minute, title, body)
            AutoReminderPrefs.markScheduled(ctx)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scheduleOnce()
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            scheduleOnce()
        } else {
            onGranted = { scheduleOnce() }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun ensureExamReminder(
        hour: Int = 7,
        minute: Int = 0,
        title: String = "FlashLearn"
    ) {
        val ctx = activity.applicationContext

        fun scheduleOnce() {
            if (ExamReminderPrefs.wasScheduled(ctx)) return
            ExamReminderScheduler.schedule(ctx, hour, minute, title)
            ExamReminderPrefs.markScheduled(ctx)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scheduleOnce()
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            scheduleOnce()
        } else {
            onGranted = { scheduleOnce() } // reuse the same callback slot
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}