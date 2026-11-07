package com.example.habitup.ui.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.habitup.data.model.Habit
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun man_hinh_rong_hien_dung_thong_bao() {
        composeRule.setContent {
            TodayScreenContent(
                state = TodayUiState(items = emptyList(), isLoading = false),
                showNotificationBanner = false,
                onToggle = {},
                onHabitClick = {},
                onAddHabit = {},
                onOpenSettings = {},
                onOpenNotificationSettings = {},
            )
        }

        composeRule.onNodeWithText("Chưa có thói quen nào cho hôm nay. Bấm \"Thói quen\" để tạo.")
            .assertIsDisplayed()
    }

    @Test
    fun bam_checkbox_goi_dung_callback_toggle() {
        var toggledId: String? = null
        val habit = Habit(id = "h1", name = "Uống nước", emoji = "💧")

        composeRule.setContent {
            TodayScreenContent(
                state = TodayUiState(items = listOf(TodayItem(habit, doneToday = false)), isLoading = false),
                showNotificationBanner = false,
                onToggle = { toggledId = it.habit.id },
                onHabitClick = {},
                onAddHabit = {},
                onOpenSettings = {},
                onOpenNotificationSettings = {},
            )
        }

        composeRule.onNodeWithText("Uống nước").assertIsDisplayed()
        composeRule.onNodeWithText("Hoàn thành 0/1 hôm nay").assertIsDisplayed()
        composeRule.onNode(isToggleable()).performClick()
        assert(toggledId == "h1")
    }
}
