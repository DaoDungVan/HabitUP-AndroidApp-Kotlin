package com.example.habitup.ui.login

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitup.data.auth.AuthRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** activityContext chỉ dùng trong thời gian gọi, KHÔNG lưu lại. */
    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val error: String? = try {
                authRepository.signInWithGoogle(activityContext)
                null
            } catch (e: GetCredentialCancellationException) {
                null
            } catch (e: NoCredentialException) {
                "Không tìm thấy tài khoản Google trên thiết bị. Hãy thêm tài khoản trong Cài đặt."
            } catch (e: GetCredentialException) {
                "Không thể đăng nhập Google (${e.type}). Kiểm tra SHA-1 và Web client ID."
            } catch (e: GoogleIdTokenParsingException) {
                "Phản hồi từ Google không hợp lệ, hãy thử lại."
            } catch (e: FirebaseNetworkException) {
                "Không có kết nối mạng."
            } catch (e: FirebaseAuthException) {
                "Firebase từ chối đăng nhập: ${e.localizedMessage}"
            }
            _uiState.update { it.copy(isLoading = false, errorMessage = error) }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
