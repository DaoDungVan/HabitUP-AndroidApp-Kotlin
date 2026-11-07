package com.example.habitup.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.habitup.data.HabitRepository
import com.example.habitup.notification.HabitNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Nút "Đã làm" trên notification — ghi completion hôm nay rồi ẩn notification, không cần mở app. */
class MarkDoneReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun habitRepository(): HabitRepository
        fun habitNotifier(): HabitNotifier
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_DONE) return
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ReceiverEntryPoint::class.java,
        )
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                entryPoint.habitRepository().setCompleted(habitId, LocalDate.now(), true)
                entryPoint.habitNotifier().cancel(habitId)
            } catch (e: Exception) {
                Log.e("MarkDoneReceiver", "Không đánh dấu được", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.habitup.ACTION_MARK_DONE"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
