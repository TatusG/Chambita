package com.chambita.app.ui.auth

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val rol: String) : LoginState()
    data class Error(val message: String) : LoginState()
    object ResetEmailSent : LoginState()
}
