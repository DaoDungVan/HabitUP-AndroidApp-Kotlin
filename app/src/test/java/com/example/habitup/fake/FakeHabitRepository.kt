package com.example.habitup.fake

import com.example.habitup.data.HabitRepository
import com.example.habitup.data.model.Completion
import com.example.habitup.data.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Repository giả trong bộ nhớ — dùng cho unit test ViewModel, không đụng Firestore thật. */
class FakeHabitRepository : HabitRepository {
    private val habits = MutableStateFlow<List<Habit>>(emptyList())
    private val completions = MutableStateFlow<List<Completion>>(emptyList())

    val setCompletedCalls = mutableListOf<Triple<String, LocalDate, Boolean>>()

    fun setHabits(value: List<Habit>) { habits.value = value }
    fun setCompletions(value: List<Completion>) { completions.value = value }

    override fun observeHabits(): Flow<List<Habit>> = habits.asStateFlow()

    override fun observeHabit(habitId: String): Flow<Habit?> =
        habits.map { list -> list.firstOrNull { it.id == habitId } }

    override fun observeCompletions(habitId: String): Flow<List<Completion>> =
        completions.map { list -> list.filter { it.habitId == habitId } }

    override fun observeTodayCompletedIds(today: LocalDate): Flow<Set<String>> =
        completions.map { list -> list.filter { it.date == today.toString() }.map { it.habitId }.toSet() }

    override suspend fun getHabit(habitId: String): Habit? = habits.value.firstOrNull { it.id == habitId }

    override suspend fun addHabit(habit: Habit) {
        habits.value = habits.value + habit.copy(id = "generated-${habits.value.size}")
    }

    override suspend fun updateHabit(habit: Habit) {
        habits.value = habits.value.map { if (it.id == habit.id) habit else it }
    }

    override suspend fun archiveHabit(habitId: String) {
        habits.value = habits.value.map { if (it.id == habitId) it.copy(archived = true) else it }
    }

    override suspend fun deleteHabit(habitId: String) {
        habits.value = habits.value.filterNot { it.id == habitId }
    }

    override suspend fun setCompleted(habitId: String, date: LocalDate, completed: Boolean) {
        setCompletedCalls += Triple(habitId, date, completed)
        completions.value = if (completed) {
            completions.value + Completion(id = "${habitId}_$date", habitId = habitId, date = date.toString())
        } else {
            completions.value.filterNot { it.habitId == habitId && it.date == date.toString() }
        }
    }
}
