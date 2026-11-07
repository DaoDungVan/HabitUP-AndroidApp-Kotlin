package com.example.habitup.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Phát user hiện tại mỗi khi trạng thái đăng nhập thay đổi; null = chưa đăng nhập. */
    val currentUser: Flow<FirebaseUser?>

    /** Hiện UI chọn tài khoản Google rồi đăng nhập Firebase. Cần Activity context. */
    suspend fun signInWithGoogle(activityContext: Context): FirebaseUser

    /** Đăng xuất Firebase và xoá trạng thái của Credential Manager. */
    suspend fun signOut()
}
