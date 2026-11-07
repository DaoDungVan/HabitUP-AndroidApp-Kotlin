package com.example.habitup.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private val DAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN") // index 0 = DayOfWeek.value 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFormScreen(
    onBack: () -> Unit,
    viewModel: HabitFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Sửa thói quen" else "Thói quen mới") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Tên thói quen *") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text("Biểu tượng", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EMOJI_CHOICES.forEach { emoji ->
                        FilterChip(
                            selected = state.emoji == emoji,
                            onClick = { viewModel.onEmojiChange(emoji) },
                            label = { Text(emoji) },
                            modifier = Modifier.wrapContentWidth(),
                        )
                    }
                }
            }

            Column {
                Text("Lặp lại vào", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val day = index + 1
                        FilterChip(
                            selected = day in state.scheduleDays,
                            onClick = { viewModel.toggleDay(day) },
                            label = { Text(label) },
                        )
                    }
                }
                state.daysError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nhắc nhở", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = state.reminderEnabled, onCheckedChange = viewModel::onReminderEnabledChange)
                }
                if (state.reminderEnabled) {
                    Text(
                        "Từ ${state.reminderHour}:00",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Slider(
                        value = state.reminderHour.toFloat(),
                        onValueChange = { viewModel.onReminderHourChange(it.roundToInt()) },
                        valueRange = 0f..23f,
                        steps = 22,
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.isEdit) "Lưu thay đổi" else "Tạo thói quen")
                }
            }
        }
    }
}
