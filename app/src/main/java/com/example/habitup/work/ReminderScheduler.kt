package com.example.habitup.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Gọi lúc app khởi động: đảm bảo đã lên lịch, không phá chu kỳ nếu đã có (KEEP). */
    fun scheduleIfNeeded() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Chạy ngay một lần, bỏ qua điều kiện giờ — để kiểm thử nhanh. */
    fun runNowForTesting() {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderWorker.KEY_FORCE to true))
            .build()
        workManager.enqueueUniqueWork(TEST_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val PERIODIC_NAME = "habit-reminder-check"
        private const val TEST_NAME = "habit-reminder-check-test"
    }
}
