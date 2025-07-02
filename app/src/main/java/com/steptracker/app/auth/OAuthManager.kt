package com.steptracker.app.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OAuthManager @Inject constructor(
    private val context: Context
) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private lateinit var googleSignInClient: GoogleSignInClient
    
    fun initializeGoogleSignIn(webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    suspend fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.await()
            val user = User(
                id = account.id ?: "",
                email = account.email ?: "",
                name = account.displayName ?: "",
                photoUrl = account.photoUrl?.toString(),
                provider = AuthProvider.GOOGLE
            )
            _authState.value = AuthState.Authenticated(user)
        } catch (e: ApiException) {
            _authState.value = AuthState.Error("Google sign-in failed: ${e.statusCode}")
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Authentication failed: ${e.message}")
        }
    }
    
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            _authState.value = AuthState.NotAuthenticated
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Sign out failed: ${e.message}")
        }
    }
    
    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }
    
    fun isAuthenticated(): Boolean {
        return _authState.value is AuthState.Authenticated
    }
}

sealed class AuthState {
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object Loading : AuthState()
}

data class User(
    val id: String,
    val email: String,
    val name: String,
    val photoUrl: String?,
    val provider: AuthProvider
)

enum class AuthProvider {
    GOOGLE,
    EMAIL
} 