package com.example.llpclient.view.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.llpclient.data.local.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = repository.isLoggedIn()
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            repository.login(username, password)
                .onSuccess {
                    _loginState.value = LoginState.Success
                    _isLoggedIn.value = true
                }
                .onFailure {
                    _loginState.value = LoginState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
            _loginState.value = LoginState.Initial
        }
    }
}