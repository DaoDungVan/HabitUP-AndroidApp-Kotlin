package com.example.habitup.ui.today

import com.example.habitup.data.model.Habit
import com.example.habitup.fake.FakeHabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * uiState dùng SharingStarted.WhileSubscribed: phải có collector đang chạy thì flow nguồn mới
 * khởi động, nên mỗi test tự collect trong backgroundScope trước khi đọc .value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeHabitRepository

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        repository = FakeHabitRepository()
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `chi hien habit toi han hom nay`() = runTest {
        val today = LocalDate.now().dayOfWeek.value
        val otherDay = if (today == 1) 2 else 1
        repository.setHabits(
            listOf(
                Habit(id = "a", name = "Uống nước", scheduleDays = listOf(today)),
                Habit(id = "b", name = "Chạy bộ", scheduleDays = listOf(otherDay)),
            ),
        )
        val viewModel = TodayViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("a", state.items.first().habit.id)
    }

    @Test
    fun `toggle goi repository voi ngay hom nay`() = runTest {
        repository.setHabits(listOf(Habit(id = "a", name = "Đọc sách", scheduleDays = (1..7).toList())))
        val viewModel = TodayViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.first()
        assertTrue(!item.doneToday)
        viewModel.toggle(item)
        advanceUntilIdle()

        assertEquals(1, repository.setCompletedCalls.size)
        val (habitId, date, completed) = repository.setCompletedCalls.first()
        assertEquals("a", habitId)
        assertEquals(LocalDate.now(), date)
        assertTrue(completed)
    }

    @Test
    fun `doneCount dem dung so habit da hoan thanh`() = runTest {
        repository.setHabits(
            listOf(
                Habit(id = "a", name = "A", scheduleDays = (1..7).toList()),
                Habit(id = "b", name = "B", scheduleDays = (1..7).toList()),
            ),
        )
        val viewModel = TodayViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.toggle(viewModel.uiState.value.items.first { it.habit.id == "a" })
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.doneCount)
        assertEquals(2, viewModel.uiState.value.items.size)
    }
}
