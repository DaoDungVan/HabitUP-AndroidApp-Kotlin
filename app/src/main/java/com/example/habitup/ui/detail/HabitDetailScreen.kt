package com.example.habitup.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habitup.data.streak.DayStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.habit?.let { "${it.emoji} ${it.name}" } ?: "Chi tiết") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    state.habit?.let { habit ->
                        IconButton(onClick = { onEdit(habit.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Sửa")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Xoá")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> Column(
                Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCard("🔥", "${state.streak.currentStreak}", "Chuỗi hiện tại")
                    StatCard("🏆", "${state.streak.longestStreak}", "Kỷ lục")
                    StatCard("📊", "${(state.streak.completionRate * 100).toInt()}%", "Tỉ lệ")
                }

                Column {
                    Text("$HISTORY_TITLE (${state.recentDays.size} ngày)", style = MaterialTheme.typography.titleMedium)
                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.recentDays) { (date, status) ->
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(statusColor(status)),
                            )
                        }
                    }
                }

                Button(onClick = viewModel::toggleToday, modifier = Modifier.fillMaxWidth()) {
                    val doneToday = state.recentDays.lastOrNull()?.second == DayStatus.DONE
                    Text(if (doneToday) "Bỏ đánh dấu hôm nay" else "Đánh dấu hoàn thành hôm nay")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá thói quen?") },
            text = { Text("\"${state.habit?.name}\" và toàn bộ lịch sử sẽ bị xoá, không khôi phục được.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); confirmDelete = false }) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } },
        )
    }
}

private const val HISTORY_TITLE = "Lịch sử"

@Composable
private fun StatCard(emoji: String, value: String, label: String) {
    Card {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun statusColor(status: DayStatus): Color = when (status) {
    DayStatus.DONE -> Color(0xFF2E7D4F)
    DayStatus.MISSED -> Color(0xFFD9772B)
    DayStatus.PENDING_TODAY -> Color(0xFFBFC6FF)
    DayStatus.NOT_DUE -> Color(0xFFE0E0E0)
}
