package com.example.habitup.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Một document trong collection "users/{uid}/completions" — đánh dấu một habit đã hoàn thành
 * vào một ngày cụ thể. Id document được đặt CỐ ĐỊNH là "{habitId}_{date}" (Bước HabitRepositoryImpl)
 * để đánh dấu hai lần trong cùng một ngày là ghi đè, không tạo bản ghi trùng.
 */
data class Completion(
    @DocumentId val id: String = "",
    val habitId: String = "",
    /** Định dạng ISO "yyyy-MM-dd" (LocalDate.toString()), dùng để truy vấn và tính streak. */
    val date: String = "",
    @ServerTimestamp val completedAt: Timestamp? = null,
)
