package com.example.habitup.data.streak

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val everyDay = (1..7).toList()
    private val today = LocalDate.of(2026, 11, 8) // Chủ Nhật

    @Test
    fun `current streak dem lui dung so ngay lien tiep`() {
        val completed = setOf(
            today, today.minusDays(1), today.minusDays(2),
            // today.minusDays(3) THIẾU -> chuỗi dừng ở đây
        )
        val streak = StreakCalculator.currentStreak(everyDay, completed, today.minusDays(10), today)
        assertEquals(3, streak)
    }

    @Test
    fun `hom nay chua danh dau khong lam dut chuoi dang co`() {
        val completed = setOf(today.minusDays(1), today.minusDays(2)) // hôm nay CHƯA đánh dấu
        val streak = StreakCalculator.currentStreak(everyDay, completed, today.minusDays(10), today)
        assertEquals(2, streak)
    }

    @Test
    fun `ngay khong thuoc lich bi bo qua khong anh huong chuoi`() {
        val mondayWednesdayFriday = listOf(1, 3, 5)
        // today 2026-11-08 là Chủ Nhật (7) -> không tới hạn, bỏ qua.
        // Thứ Sáu 2026-11-06 (5) và Thứ Tư 2026-11-04 (3) đã hoàn thành.
        val completed = setOf(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 4))
        val streak = StreakCalculator.currentStreak(mondayWednesdayFriday, completed, today.minusDays(20), today)
        assertEquals(2, streak)
    }

    @Test
    fun `longest streak tim dung chuoi dai nhat tung dat duoc`() {
        val createdOn = today.minusDays(6)
        // Hoàn thành 3 ngày liên tiếp, bỏ 1 ngày, rồi hoàn thành 2 ngày -> dài nhất là 3.
        val completed = setOf(
            createdOn, createdOn.plusDays(1), createdOn.plusDays(2),
            createdOn.plusDays(4), createdOn.plusDays(5),
        )
        val longest = StreakCalculator.longestStreak(everyDay, completed, createdOn, today)
        assertEquals(3, longest)
    }

    @Test
    fun `completion rate tinh dung ti le tren so ngay toi han`() {
        val createdOn = today.minusDays(3) // 4 ngày tới hạn: createdOn..today
        val completed = setOf(createdOn, createdOn.plusDays(1))
        val rate = StreakCalculator.completionRate(everyDay, completed, createdOn, today)
        assertEquals(0.5f, rate, 0.001f)
    }
}
