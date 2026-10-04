package com.voicereminder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicereminder.network.AuthManager
import com.voicereminder.network.VoiceboxApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d("AuthViewModel", "Attempting login to ${AuthManager.baseUrl}auth/token")
                val response = VoiceboxApiClient.get().login(email.trim(), password)
                when {
                    response.isSuccessful && response.body() != null -> {
                        AuthManager.accessToken = response.body()!!.accessToken
                        Log.d("AuthViewModel", "Login successful")
                        _authState.value = AuthState.Success
                    }
                    response.code() == 404 -> {
                        val url = AuthManager.baseUrl
                        _authState.value = AuthState.Error(
                            "Server not found (404). Check that your backend is running at:\n$url\n\nUpdate server URL below if needed."
                        )
                    }
                    response.code() == 400 -> {
                        _authState.value = AuthState.Error("Incorrect email or password. Please try again.")
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string() ?: "No response body"
                        _authState.value = AuthState.Error("Login failed (${response.code()}): $errorBody")
                    }
                }
            } catch (e: ConnectException) {
                _authState.value = AuthState.Error("Cannot connect to server at ${AuthManager.baseUrl}\n\nMake sure:\n• Backend is running\n• Phone and Mac are on same WiFi\n• IP address is correct")
            } catch (e: SocketTimeoutException) {
                _authState.value = AuthState.Error("Connection timed out. Server may be slow or unreachable.")
            } catch (e: UnknownHostException) {
                _authState.value = AuthState.Error("Unknown host. Check the server IP in settings below.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error", e)
                _authState.value = AuthState.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    fun updateServerUrl(url: String) {
        AuthManager.baseUrl = url
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
