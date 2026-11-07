package com.example.habitup.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.habitup.data.HabitRepository
import com.example.habitup.data.model.Habit
import com.example.habitup.data.streak.DayStatus
import com.example.habitup.data.streak.StreakCalculator
import com.example.habitup.data.streak.StreakInfo
import com.example.habitup.data.toUserMessage
import com.example.habitup.ui.navigation.HabitDetail
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private const val HISTORY_DAYS = 30

data class HabitDetailUiState(
    val habit: Habit? = null,
    val streak: StreakInfo = StreakInfo(0, 0, 0f),
    val recentDays: List<Pair<LocalDate, DayStatus>> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

private fun Timestamp.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(toDate().time).atZone(ZoneId.systemDefault()).toLocalDate()

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
) : ViewModel() {

    private val habitId: String = savedStateHandle.toRoute<HabitDetail>().habitId

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    val uiState: StateFlow<HabitDetailUiState> = combine(
        repository.observeHabit(habitId),
        repository.observeCompletions(habitId),
    ) { habit, completions ->
        if (habit == null) {
            HabitDetailUiState(isLoading = false, error = "Thói quen đã bị xoá")
        } else {
            val today = LocalDate.now()
            val completedDates = completions
                .mapNotNull { c -> runCatching { LocalDate.parse(c.date) }.getOrNull() }
                .toSet()
            val createdOn = habit.createdAt?.toLocalDate() ?: today
            HabitDetailUiState(
                habit = habit,
                streak = StreakCalculator.calculate(habit.scheduleDays, completedDates, createdOn, today),
                recentDays = StreakCalculator.recentDayStatuses(habit.scheduleDays, completedDates, today, HISTORY_DAYS),
                isLoading = false,
            )
        }
    }
        .catch { e -> emit(HabitDetailUiState(isLoading = false, error = e.toUserMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    fun toggleToday() {
        val habit = uiState.value.habit ?: return
        val doneToday = uiState.value.recentDays.lastOrNull()?.second == DayStatus.DONE
        viewModelScope.launch {
            runCatching { repository.setCompleted(habit.id, LocalDate.now(), !doneToday) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                repository.deleteHabit(habitId)
                _deleted.update { true }
            } catch (e: Exception) {
                // Lỗi xoá hiếm khi xảy ra; người dùng có thể thử lại từ nút thùng rác.
            }
        }
    }
}
