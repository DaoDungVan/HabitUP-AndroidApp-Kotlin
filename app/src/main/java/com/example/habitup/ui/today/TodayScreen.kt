package com.example.habitup.ui.today

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun android.content.Context.notificationsAllowed(): Boolean {
    val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    return permissionOk && NotificationManagerCompat.from(this).areNotificationsEnabled()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddHabit: () -> Unit,
    onHabitClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.notificationsAllowed()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed = context.notificationsAllowed() }

    var askedPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!allowed && !askedPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askedPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    TodayScreenContent(
        state = state,
        showNotificationBanner = !allowed,
        onToggle = viewModel::toggle,
        onHabitClick = onHabitClick,
        onAddHabit = onAddHabit,
        onOpenSettings = onOpenSettings,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
    )
}

/** Tách khỏi TodayScreen để test Compose UI không cần Hilt: chỉ nhận state + callback thuần. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreenContent(
    state: TodayUiState,
    showNotificationBanner: Boolean,
    onToggle: (TodayItem) -> Unit,
    onHabitClick: (String) -> Unit,
    onAddHabit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hôm nay") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHabit,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Thói quen") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (showNotificationBanner) {
                NotificationBanner(onOpenSettings = onOpenNotificationSettings)
            }
            if (state.items.isNotEmpty()) {
                Text(
                    "Hoàn thành ${state.doneCount}/${state.items.size} hôm nay",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            // Centered/LazyColumn là sibling của tiêu đề tiến độ trong Column không cuộn được:
            // phải dùng weight(1f), nếu không sẽ bị đo bằng chiều cao của CẢ Column và tràn màn hình.
            when {
                state.isLoading && state.items.isEmpty() ->
                    Centered(Modifier.weight(1f)) { CircularProgressIndicator() }
                state.error != null && state.items.isEmpty() ->
                    Centered(Modifier.weight(1f)) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                state.items.isEmpty() ->
                    Centered(Modifier.weight(1f)) {
                        Text("Chưa có thói quen nào cho hôm nay. Bấm \"Thói quen\" để tạo.")
                    }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.habit.id }) { item ->
                        HabitRow(
                            item = item,
                            onToggle = { onToggle(item) },
                            onClick = { onHabitClick(item.habit.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(item: TodayItem, onToggle: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.habit.emoji, style = MaterialTheme.typography.headlineSmall)
            Text(
                item.habit.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Checkbox(checked = item.doneToday, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun NotificationBanner(onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Thông báo đang tắt — HabitUp không thể nhắc bạn làm thói quen.")
            TextButton(onClick = onOpenSettings) { Text("Mở cài đặt thông báo") }
        }
    }
}

@Composable
private fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}
