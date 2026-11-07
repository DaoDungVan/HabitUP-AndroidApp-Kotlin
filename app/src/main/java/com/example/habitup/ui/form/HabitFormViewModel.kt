package com.example.habitup.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.habitup.data.HabitRepository
import com.example.habitup.data.model.Habit
import com.example.habitup.data.toUserMessage
import com.example.habitup.ui.navigation.HabitForm
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val EMOJI_CHOICES = listOf("✅", "💧", "🏃", "📚", "🧘", "💪", "🥗", "😴", "✍️", "🚭", "🎸", "🧹")

data class HabitFormUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val emoji: String = EMOJI_CHOICES.first(),
    val scheduleDays: Set<Int> = (1..7).toSet(),   // DayOfWeek.value: 1=Hai .. 7=CN
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val nameError: String? = null,
    val daysError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class HabitFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
) : ViewModel() {

    private val habitId: String? = savedStateHandle.toRoute<HabitForm>().habitId
    private var originalCreatedAt: Timestamp? = null

    private val _uiState = MutableStateFlow(HabitFormUiState(isEdit = habitId != null))
    val uiState: StateFlow<HabitFormUiState> = _uiState.asStateFlow()

    init {
        if (habitId != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val habit = repository.getHabit(habitId)
                    if (habit == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy thói quen") }
                    } else {
                        originalCreatedAt = habit.createdAt
                        _uiState.update {
                            it.copy(
                                name = habit.name,
                                emoji = habit.emoji,
                                scheduleDays = habit.scheduleDays.toSet(),
                                reminderEnabled = habit.reminderEnabled,
                                reminderHour = habit.reminderHour,
                                isLoading = false,
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onEmojiChange(value: String) = _uiState.update { it.copy(emoji = value) }
    fun onReminderEnabledChange(value: Boolean) = _uiState.update { it.copy(reminderEnabled = value) }
    fun onReminderHourChange(value: Int) = _uiState.update { it.copy(reminderHour = value) }

    fun toggleDay(day: Int) = _uiState.update {
        val days = if (day in it.scheduleDays) it.scheduleDays - day else it.scheduleDays + day
        it.copy(scheduleDays = days, daysError = null)
    }

    fun save() {
        val s = _uiState.value
        val nameError = if (s.name.isBlank()) "Nhập tên thói quen" else null
        val daysError = if (s.scheduleDays.isEmpty()) "Chọn ít nhất một ngày" else null
        if (nameError != null || daysError != null) {
            _uiState.update { it.copy(nameError = nameError, daysError = daysError) }
            return
        }

        val habit = Habit(
            id = habitId.orEmpty(),
            name = s.name.trim(),
            emoji = s.emoji,
            scheduleDays = s.scheduleDays.sorted(),
            reminderEnabled = s.reminderEnabled,
            reminderHour = s.reminderHour,
            archived = false,
            createdAt = originalCreatedAt, // null ở habit mới -> server tự điền giờ server khi add()
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                if (habitId == null) repository.addHabit(habit) else repository.updateHabit(habit)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }
}
