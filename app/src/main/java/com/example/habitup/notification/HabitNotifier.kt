package com.example.habitup.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habitup.MainActivity
import com.example.habitup.R
import com.example.habitup.receiver.MarkDoneReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_REMINDER = "habit_reminder"
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_REMINDER, "Nhắc thói quen", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Nhắc hoàn thành thói quen trong ngày" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPostNotifications(): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** id theo habitId.hashCode(): mỗi habit có một notification riêng, cập nhật/huỷ độc lập. */
    fun showReminder(habitId: String, habitName: String, emoji: String) {
        if (!canPostNotifications()) return

        val doneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
            action = MarkDoneReceiver.ACTION_MARK_DONE
            putExtra(MarkDoneReceiver.EXTRA_HABIT_ID, habitId)
        }
        val donePending = PendingIntent.getBroadcast(
            context, habitId.hashCode(), doneIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_flame)
            .setContentTitle("$emoji $habitName")
            .setContentText("Hôm nay bạn đã làm việc này chưa?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_flame, "Đã làm", donePending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(habitId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Quyền bị thu hồi đúng lúc này — bỏ qua
        }
    }

    fun cancel(habitId: String) {
        NotificationManagerCompat.from(context).cancel(habitId.hashCode())
    }
}
