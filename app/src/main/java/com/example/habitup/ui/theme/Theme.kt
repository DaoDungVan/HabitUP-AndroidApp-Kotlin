package com.example.habitup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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

// Bo góc lớn hơn mặc định M3 — tạo cảm giác "mềm", hiện đại hơn thay vì các ô vuông cứng.
val HabitUpShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
        shapes = HabitUpShapes,
        content = content,
    )
}
