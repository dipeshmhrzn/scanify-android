package com.scanify.app.presentation.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scanify.app.R
import com.scanify.app.presentation.MainActivity
import kotlinx.coroutines.delay
import androidx.core.graphics.createBitmap
import kotlin.time.Duration.Companion.milliseconds

object NotificationHelper {
    const val CHANNEL_FILE_OPERATIONS = "file_operations"
    const val CHANNEL_FILE_OPERATION_RESULTS = "file_operation_results"
    const val CHANNEL_REMINDERS = "reminders"

    const val PROGRESS_NOTIFICATION_ID = 1001
    const val COMPLETION_NOTIFICATION_ID = 1002
    const val REMINDER_NOTIFICATION_ID = 2001

    @Volatile
    private var cachedLargeIcon: Bitmap? = null

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FILE_OPERATIONS,
                "File operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress for imports, saves, and backups"
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FILE_OPERATION_RESULTS,
                "Import / save / backup results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lets you know when an import, save, or backup finishes"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Occasional reminders to scan a document"
            }
        )
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPercent: Int,
        indeterminate: Boolean = false
    ): android.app.Notification =
        NotificationCompat.Builder(context, CHANNEL_FILE_OPERATIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(getLargeIconBitmap(context))
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent, indeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    suspend fun showCompletionNotification(context: Context, title: String, message: String, isSuccess: Boolean = true) {
        if (!canPostNotifications(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = if (isSuccess) R.drawable.ic_notification_success else R.drawable.ic_notification

        val notification = NotificationCompat.Builder(context, CHANNEL_FILE_OPERATION_RESULTS)
            .setSmallIcon(icon)
            .setLargeIcon(getLargeIconBitmap(context))
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        var posted = false
        NotificationManagerCompat.from(context).let { manager ->
            try {
                manager.notify(COMPLETION_NOTIFICATION_ID, notification)
                posted = true
            } catch (e: SecurityException) {
                // Permission was revoked in the brief window between canPostNotifications()
                // above and this call - safe to just skip posting rather than crash.
            }
        }

        if (posted) delay(300.milliseconds)
    }

    fun showReminderNotification(context: Context) {
        if (!canPostNotifications(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(getLargeIconBitmap(context))
            .setContentTitle("Got a document to scan?")
            .setContentText("Open Scanify to digitize it in seconds.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).let { manager ->
            try {
                manager.notify(REMINDER_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // Same race-condition guard as showCompletionNotification above.
            }
        }
    }

    private fun getLargeIconBitmap(context: Context): Bitmap? {
        cachedLargeIcon?.let { return it }

        return try {
            val drawable = ContextCompat.getDrawable(context, context.applicationInfo.icon)
                ?: return null
            val bitmap = drawableToBitmap(drawable)
            cachedLargeIcon = bitmap
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val size = 192
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}