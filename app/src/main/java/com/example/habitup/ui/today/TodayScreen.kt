package com.example.habitup.ui.today

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habitup.ui.theme.FlameOrange40

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
                title = { Text("HabitUp", style = MaterialTheme.typography.titleLarge) },
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (showNotificationBanner) {
                NotificationBanner(onOpenSettings = onOpenNotificationSettings)
            }
            if (state.items.isNotEmpty() || state.isLoading) {
                ProgressHero(doneCount = state.doneCount, total = state.items.size)
            }
            // Centered/LazyColumn là sibling của ProgressHero trong Column không cuộn được:
            // phải dùng weight(1f), nếu không sẽ bị đo bằng chiều cao của CẢ Column và tràn màn hình.
            when {
                state.isLoading && state.items.isEmpty() ->
                    Centered(Modifier.weight(1f)) { CircularProgressIndicator(color = FlameOrange40) }
                state.error != null && state.items.isEmpty() ->
                    Centered(Modifier.weight(1f)) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                state.items.isEmpty() -> Centered(Modifier.weight(1f)) { EmptyToday() }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
private fun ProgressHero(doneCount: Int, total: Int) {
    val progress = if (total == 0) 0f else doneCount.toFloat() / total
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFA559), Color(0xFFC2410C))))
            .padding(20.dp),
    ) {
        Column {
            Text("🔥 Hôm nay", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$doneCount/$total",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(Modifier.width(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f),
                )
            }
        }
    }
}

@Composable
private fun HabitRow(item: TodayItem, onToggle: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.habit.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                item.habit.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 14.dp),
            )
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.doneToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .toggleable(value = item.doneToday, role = Role.Checkbox, onValueChange = { onToggle() }),
                contentAlignment = Alignment.Center,
            ) {
                if (item.doneToday) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Đã hoàn thành",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyToday() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("🔥", style = MaterialTheme.typography.displayMedium)
        }
        Text("Chưa có thói quen nào", style = MaterialTheme.typography.titleMedium)
        Text(
            "Bấm \"Thói quen\" để bắt đầu chuỗi ngày đầu tiên của bạn",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun NotificationBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
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
