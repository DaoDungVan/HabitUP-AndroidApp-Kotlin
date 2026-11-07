package com.example.habitup.data

import com.example.habitup.data.model.Completion
import com.example.habitup.data.model.Habit
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

/**
 * Dữ liệu lưu dưới "users/{uid}/habits" và "users/{uid}/completions" — tách theo người dùng,
 * bảo vệ bằng Security Rules (chỉ chủ tài khoản đọc/ghi được, xem firestore.rules ở gốc repo).
 */
class HabitRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : HabitRepository {

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Chưa đăng nhập")

    private fun habitsCollection(): CollectionReference =
        db.collection("users").document(requireUid()).collection("habits")

    private fun completionsCollection(): CollectionReference =
        db.collection("users").document(requireUid()).collection("completions")

    override fun observeHabits(): Flow<List<Habit>> =
        habitsCollection().whereEqualTo("archived", false).snapshots()
            .map { snapshot ->
                snapshot.toObjects(Habit::class.java)
                    .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }

    override fun observeHabit(habitId: String): Flow<Habit?> =
        habitsCollection().document(habitId).snapshots()
            .map { it.toObject(Habit::class.java) }

    override fun observeCompletions(habitId: String): Flow<List<Completion>> =
        completionsCollection().whereEqualTo("habitId", habitId).snapshots()
            .map { it.toObjects(Completion::class.java) }

    override fun observeTodayCompletedIds(today: LocalDate): Flow<Set<String>> =
        completionsCollection().whereEqualTo("date", today.toString()).snapshots()
            .map { snapshot -> snapshot.toObjects(Completion::class.java).map { it.habitId }.toSet() }

    override suspend fun getHabit(habitId: String): Habit? =
        habitsCollection().document(habitId).get().await().toObject(Habit::class.java)

    override suspend fun addHabit(habit: Habit) {
        habitsCollection().add(habit).await()
    }

    override suspend fun updateHabit(habit: Habit) {
        habitsCollection().document(habit.id).set(habit).await()
    }

    override suspend fun archiveHabit(habitId: String) {
        habitsCollection().document(habitId).update("archived", true).await()
    }

    override suspend fun deleteHabit(habitId: String) {
        habitsCollection().document(habitId).delete().await()
        // Dọn luôn completion của habit vừa xoá, tránh rác tích tụ trong collection dùng chung.
        val orphaned = completionsCollection().whereEqualTo("habitId", habitId).get().await()
        if (!orphaned.isEmpty) {
            val batch = db.batch()
            orphaned.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    override suspend fun setCompleted(habitId: String, date: LocalDate, completed: Boolean) {
        // Id cố định "{habitId}_{date}": bấm hai lần trong ngày là ghi đè/idempotent, không trùng bản ghi.
        val ref = completionsCollection().document("${habitId}_$date")
        if (completed) {
            ref.set(Completion(habitId = habitId, date = date.toString())).await()
        } else {
            ref.delete().await()
        }
    }
}
