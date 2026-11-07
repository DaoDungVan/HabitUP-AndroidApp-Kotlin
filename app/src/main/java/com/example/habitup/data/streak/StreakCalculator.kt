package com.example.habitup.data.streak

import java.time.LocalDate

data class StreakInfo(
    /** Số ngày liên tiếp (tính theo đúng lịch của habit) vừa hoàn thành, tính lùi từ hôm nay. */
    val currentStreak: Int,
    /** Chuỗi dài nhất từng đạt được, kể từ ngày tạo habit tới hôm nay. */
    val longestStreak: Int,
    /** Tỉ lệ hoàn thành trong số các ngày phải làm, từ ngày tạo tới hôm nay. 0f nếu chưa có ngày nào tới hạn. */
    val completionRate: Float,
)

enum class DayStatus { DONE, MISSED, NOT_DUE, PENDING_TODAY }

/**
 * Tính streak theo "ngày phải làm" (scheduleDays) của một habit, không phải theo mọi ngày lịch.
 * Ngày không thuộc lịch của habit được bỏ qua hoàn toàn — không cộng, không làm đứt chuỗi.
 */
object StreakCalculator {

    private fun isDue(date: LocalDate, scheduleDays: List<Int>): Boolean =
        scheduleDays.isEmpty() || date.dayOfWeek.value in scheduleDays

    /**
     * Đếm lùi từ hôm nay: ngày phải làm mà có completion thì +1 rồi lùi tiếp; ngày phải làm mà
     * THIẾU completion thì dừng ngay — TRỪ hôm nay (hôm nay chưa xong thì bỏ qua, không tính là
     * mất chuỗi, vì ngày chưa kết thúc), vẫn tiếp tục xét các ngày trước đó.
     */
    fun currentStreak(
        scheduleDays: List<Int>,
        completedDates: Set<LocalDate>,
        habitCreatedOn: LocalDate,
        today: LocalDate,
    ): Int {
        var streak = 0
        var date = today
        while (!date.isBefore(habitCreatedOn)) {
            if (isDue(date, scheduleDays)) {
                when {
                    date in completedDates -> streak++
                    date == today -> Unit // chưa hết ngày, chưa tính mất
                    else -> return streak
                }
            }
            date = date.minusDays(1)
        }
        return streak
    }

    /** Quét xuôi từ ngày tạo habit tới hôm nay, tìm chuỗi liên tiếp dài nhất từng đạt được. */
    fun longestStreak(
        scheduleDays: List<Int>,
        completedDates: Set<LocalDate>,
        habitCreatedOn: LocalDate,
        today: LocalDate,
    ): Int {
        var longest = 0
        var running = 0
        var date = habitCreatedOn
        while (!date.isAfter(today)) {
            if (isDue(date, scheduleDays)) {
                when {
                    date in completedDates -> {
                        running++
                        if (running > longest) longest = running
                    }
                    date == today -> Unit
                    else -> running = 0
                }
            }
            date = date.plusDays(1)
        }
        return longest
    }

    fun completionRate(
        scheduleDays: List<Int>,
        completedDates: Set<LocalDate>,
        habitCreatedOn: LocalDate,
        today: LocalDate,
    ): Float {
        var totalDue = 0
        var done = 0
        var date = habitCreatedOn
        while (!date.isAfter(today)) {
            if (isDue(date, scheduleDays)) {
                totalDue++
                if (date in completedDates) done++
            }
            date = date.plusDays(1)
        }
        return if (totalDue == 0) 0f else done.toFloat() / totalDue
    }

    fun calculate(
        scheduleDays: List<Int>,
        completedDates: Set<LocalDate>,
        habitCreatedOn: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): StreakInfo = StreakInfo(
        currentStreak = currentStreak(scheduleDays, completedDates, habitCreatedOn, today),
        longestStreak = longestStreak(scheduleDays, completedDates, habitCreatedOn, today),
        completionRate = completionRate(scheduleDays, completedDates, habitCreatedOn, today),
    )

    /** Trạng thái từng ngày trong [days] ngày gần nhất (kể cả hôm nay) — dùng vẽ dải lịch sử. */
    fun recentDayStatuses(
        scheduleDays: List<Int>,
        completedDates: Set<LocalDate>,
        today: LocalDate,
        days: Int,
    ): List<Pair<LocalDate, DayStatus>> =
        (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val status = when {
                !isDue(date, scheduleDays) -> DayStatus.NOT_DUE
                date in completedDates -> DayStatus.DONE
                date == today -> DayStatus.PENDING_TODAY
                else -> DayStatus.MISSED
            }
            date to status
        }
}
