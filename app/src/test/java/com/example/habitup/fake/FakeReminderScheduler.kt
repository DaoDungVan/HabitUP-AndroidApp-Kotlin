package com.example.habitup.fake

import com.example.habitup.work.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {
    var runNowCalled = false

    override fun scheduleIfNeeded() = Unit
    override fun runNowForTesting() {
        runNowCalled = true
    }
}
