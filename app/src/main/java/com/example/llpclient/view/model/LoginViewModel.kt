package com.example.llpclient.view.model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _loginOperationState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginOperationState: StateFlow<LoginState> = _loginOperationState.asStateFlow()
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    fun login(username: String, password: String) {
        if (_loginOperationState.value == LoginState.Loading) {
            return
        }
        viewModelScope.launch {
            _loginOperationState.value = LoginState.Loading
            authManager.login(username, password)
                .onSuccess { success ->
                    if (success) {
                        _loginOperationState.value = LoginState.Success
                    } else {
                        _loginOperationState.value = LoginState.Error("Login failed unexpectedly.")
                    }
                }
                .onFailure { exception ->
                    _loginOperationState.value = LoginState.Error(exception.message ?: "Unknown login error")
                }
        }
    }
    fun logout() {
        viewModelScope.launch {
            authManager.logout()
            _loginOperationState.value = LoginState.Initial
        }
    }
    fun clearLoginError() {
        if (_loginOperationState.value is LoginState.Error) {
            _loginOperationState.value = LoginState.Initial
        }
    }
}