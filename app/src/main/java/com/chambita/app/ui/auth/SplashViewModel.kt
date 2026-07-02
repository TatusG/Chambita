package com.chambita.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chambita.app.data.local.entities.UserSessionEntity
import com.chambita.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _sessionState = MutableStateFlow<UserSessionEntity?>(null)
    val sessionState: StateFlow<UserSessionEntity?> = _sessionState

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    fun checkSession() {
        viewModelScope.launch {
            val session = repository.getActiveSession()
            _sessionState.value = session
            _isReady.value = true
        }
    }
}
