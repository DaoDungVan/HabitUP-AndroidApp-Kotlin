package com.example.habitup.data

import com.example.habitup.data.model.Completion
import com.example.habitup.data.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** ViewModel chỉ biết interface này — không biết Firestore tồn tại, dễ test bằng fake. */
interface HabitRepository {
    /** Mọi habit chưa lưu trữ (archived) của người dùng hiện tại, realtime. */
    fun observeHabits(): Flow<List<Habit>>

    /** Một habit theo id, null nếu đã bị xoá. Dùng cho màn Chi tiết (tự cập nhật khi đổi cài đặt). */
    fun observeHabit(habitId: String): Flow<Habit?>

    /** Toàn bộ completion của một habit (không orderBy để khỏi cần composite index — sắp ở tầng gọi). */
    fun observeCompletions(habitId: String): Flow<List<Completion>>

    /** Tập habitId đã hoàn thành vào [today] — dùng tô dấu tích trên màn Hôm nay. */
    fun observeTodayCompletedIds(today: LocalDate): Flow<Set<String>>

    suspend fun getHabit(habitId: String): Habit?
    suspend fun addHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun archiveHabit(habitId: String)
    suspend fun deleteHabit(habitId: String)

    /** Bật/tắt hoàn thành cho một habit vào một ngày — ghi đè idempotent theo id "{habitId}_{date}". */
    suspend fun setCompleted(habitId: String, date: LocalDate, completed: Boolean)
}
