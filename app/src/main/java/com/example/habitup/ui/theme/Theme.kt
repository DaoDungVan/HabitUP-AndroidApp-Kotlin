package com.example.habitup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FlameOrange80,
    secondary = Ember80,
    tertiary = Indigo80,
)

private val LightColorScheme = lightColorScheme(
    primary = FlameOrange40,
    secondary = Ember40,
    tertiary = Indigo40,
)

@Composable
fun HabitUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Cố định bảng màu riêng của app (chủ đề lửa) thay vì Material You theo hình nền máy —
    // mọi app lab chạy chung emulator mới không còn bị trùng màu hệ thống với nhau.
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
