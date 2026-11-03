package com.jsf.app.pravin.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsf.app.pravin.data.remote.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successUid: String? = null,
    val isNewUser: Boolean = false,
    val passwordResetSent: Boolean = false
)

enum class AuthMode { SIGN_IN, SIGN_UP }

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithEmail(email.trim(), password)
            result.fold(
                onSuccess = { uid ->
                    _uiState.update { it.copy(isLoading = false, successUid = uid, isNewUser = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        when {
            email.isBlank() || password.isBlank() -> {
                _uiState.update { it.copy(error = "Please fill in all fields") }
                return
            }
            password != confirmPassword -> {
                _uiState.update { it.copy(error = "Passwords don't match") }
                return
            }
            password.length < 6 -> {
                _uiState.update { it.copy(error = "Password must be at least 6 characters") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signUpWithEmail(email.trim(), password)
            result.fold(
                onSuccess = { uid ->
                    _uiState.update { it.copy(isLoading = false, successUid = uid, isNewUser = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email to reset password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.sendPasswordReset(email.trim())
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, passwordResetSent = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun continueAsLocalUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uid = authRepository.signInAsLocalUser()
            _uiState.update { it.copy(isLoading = false, successUid = uid, isNewUser = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearPasswordResetFlag() = _uiState.update { it.copy(passwordResetSent = false) }
}
