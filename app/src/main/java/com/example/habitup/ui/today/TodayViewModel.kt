package com.example.habitup.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitup.data.HabitRepository
import com.example.habitup.data.model.Habit
import com.example.habitup.data.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayItem(val habit: Habit, val doneToday: Boolean)

data class TodayUiState(
    val items: List<TodayItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val doneCount: Int get() = items.count { it.doneToday }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: HabitRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        repository.observeHabits(),
        repository.observeTodayCompletedIds(LocalDate.now()),
    ) { habits, doneIds ->
        val todayDow = LocalDate.now().dayOfWeek.value
        val dueToday = habits.filter { it.isDueOn(todayDow) }
        TodayUiState(
            items = dueToday.map { TodayItem(it, doneToday = it.id in doneIds) },
            isLoading = false,
        )
    }
        .catch { e -> emit(TodayUiState(isLoading = false, error = e.toUserMessage())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Bật/tắt hoàn thành hôm nay cho một habit — bấm nhầm bấm lại là huỷ, không cần hộp thoại. */
    fun toggle(item: TodayItem) {
        viewModelScope.launch {
            runCatching {
                repository.setCompleted(item.habit.id, LocalDate.now(), !item.doneToday)
            }
        }
    }
}
