package com.example.habitup.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habitup.data.HabitRepository
import com.example.habitup.notification.HabitNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Chạy định kỳ (khoảng mỗi giờ — WorkManager không hẹn giờ chính xác, xem bài WorkManager).
 * Với mỗi habit chưa hoàn thành hôm nay, tới hạn hôm nay và đã qua giờ nhắc đã đặt -> hiện notification.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: HabitRepository,
    private val notifier: HabitNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false)
        if (!notifier.canPostNotifications()) return Result.success()

        val today = LocalDate.now()
        val hourNow = LocalTime.now().hour
        val habits = repository.observeHabits().first()
        val doneToday = repository.observeTodayCompletedIds(today).first()

        var notified = 0
        habits.forEach { habit ->
            val due = habit.isDueOn(today.dayOfWeek.value)
            val alreadyDone = habit.id in doneToday
            val timeReached = force || hourNow >= habit.reminderHour
            if (habit.reminderEnabled && due && !alreadyDone && timeReached) {
                notifier.showReminder(habit.id, habit.name, habit.emoji)
                notified++
            }
        }
        Log.d(TAG, "hour=$hourNow habits=${habits.size} notified=$notified force=$force")
        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
        private const val TAG = "ReminderWorker"
    }
}
