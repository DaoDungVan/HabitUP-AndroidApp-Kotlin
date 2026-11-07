package com.example.habitup.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Một document trong collection "users/{uid}/habits".
 * Mọi thuộc tính đều có giá trị mặc định: Firestore cần constructor không tham số để toObject().
 */
data class Habit(
    @DocumentId val id: String = "",
    val name: String = "",
    val emoji: String = "✅",
    /** Thứ trong tuần phải làm, theo DayOfWeek.value: 1=Thứ Hai .. 7=Chủ Nhật. Rỗng/đủ 7 = mỗi ngày. */
    val scheduleDays: List<Int> = (1..7).toList(),
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun isDueOn(dayOfWeek: Int): Boolean = scheduleDays.isEmpty() || dayOfWeek in scheduleDays
}
