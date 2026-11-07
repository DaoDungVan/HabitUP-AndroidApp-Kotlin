package com.example.habitup.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Đổi exception từ tầng Firestore/Auth thành câu dễ hiểu cho người dùng. */
fun Throwable.toUserMessage(): String = when (this) {
    is FirebaseNetworkException -> "Không có kết nối mạng."
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Bạn không có quyền với dữ liệu này."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Không kết nối được máy chủ, đang dùng dữ liệu đã lưu offline."
        else -> message ?: "Lỗi Firestore."
    }
    is IllegalStateException -> message ?: "Chưa đăng nhập."
    else -> message ?: javaClass.simpleName
}
