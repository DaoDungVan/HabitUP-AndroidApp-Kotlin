package com.example.habitup

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.habitup.notification.HabitNotifier
import com.example.habitup.work.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HabitUpApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifier: HabitNotifier
    @Inject lateinit var scheduler: ReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
        // WorkManager.getInstance() (gọi lần đầu trong scheduleIfNeeded) mở database nội bộ —
        // chạy đồng bộ trên main thread làm chậm hẳn lần vẽ UI đầu tiên (đo được tới vài giây,
        // rớt hàng chục khung hình). Màn Hôm nay không cần đợi việc này mới hiện được.
        appScope.launch { scheduler.scheduleIfNeeded() }
    }
}
