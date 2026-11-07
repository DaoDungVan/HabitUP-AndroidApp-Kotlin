package com.example.habitup.fake

import android.content.Context
import com.example.habitup.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(initialUser: FirebaseUser? = null) : AuthRepository {
    private val user = MutableStateFlow(initialUser)
    var signOutCalled = false
        private set

    override val currentUser = user.asStateFlow()

    override suspend fun signInWithGoogle(activityContext: Context): FirebaseUser =
        user.value ?: throw IllegalStateException("FakeAuthRepository chưa gán user")

    override suspend fun signOut() {
        signOutCalled = true
        user.value = null
    }
}
