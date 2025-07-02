package com.steptracker.app.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.steptracker.app.auth.OAuthManager
import com.steptracker.app.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val oAuthManager: OAuthManager,
    private val securePreferences: SecurePreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthenticationStatus()
    }
    
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            val token = securePreferences.getUserToken()
            if (token != null && oAuthManager.isAuthenticated()) {
                _uiState.value = _uiState.value.copy(isAuthenticated = true)
            }
        }
    }
    
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // In a real app, you would validate against your backend
                // For now, we'll simulate a successful login
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    // Store credentials securely
                    securePreferences.storeUserCredentials(email, password)
                    
                    // Generate and store a secure token
                    val token = oAuthManager.generateSecureToken()
                    securePreferences.storeUserToken(token)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please enter valid email and password"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Login failed: ${e.message}"
                )
            }
        }
    }
    
    fun getGoogleSignInIntent(): Intent {
        return oAuthManager.getGoogleSignInIntent()
    }
    
    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                oAuthManager.handleGoogleSignInResult(task)
                
                // Store the authentication token
                val account = task.getResult(ApiException::class.java)
                val token = account.idToken
                if (token != null) {
                    securePreferences.storeUserToken(token)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true
                )
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google sign-in failed: ${e.statusCode}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Authentication failed: ${e.message}"
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                oAuthManager.signOut()
                securePreferences.clearAllSecureData()
                _uiState.value = LoginUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Sign out failed: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
) 