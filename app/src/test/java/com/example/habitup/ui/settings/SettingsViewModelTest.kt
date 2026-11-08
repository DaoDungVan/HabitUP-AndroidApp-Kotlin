package com.example.habitup.ui.settings

import com.example.habitup.fake.FakeAuthRepository
import com.example.habitup.fake.FakeReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var reminderScheduler: FakeReminderScheduler

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        reminderScheduler = FakeReminderScheduler()
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `email la null khi chua dang nhap`() = runTest {
        val viewModel = SettingsViewModel(repository, reminderScheduler)
        backgroundScope.launch { viewModel.email.collect {} }
        advanceUntilIdle()

        assertNull(viewModel.email.value)
    }

    @Test
    fun `signOut goi dung xuong repository`() = runTest {
        val viewModel = SettingsViewModel(repository, reminderScheduler)
        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(repository.signOutCalled)
    }

    @Test
    fun `testNotificationNow goi dung xuong scheduler`() {
        val viewModel = SettingsViewModel(repository, reminderScheduler)
        viewModel.testNotificationNow()

        assertTrue(reminderScheduler.runNowCalled)
    }
}
