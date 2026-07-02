package com.chambita.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chambita.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(correo: String, contrasena: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = repository.login(correo, contrasena)
            result.onSuccess { usuario ->
                _loginState.value = LoginState.Success(usuario.rol)
            }.onFailure { exception ->
                _loginState.value = LoginState.Error(exception.message ?: "Error desconocido")
            }
        }
    }

    fun recuperarContrasena(email: String) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = LoginState.Error("Ingresa un correo válido")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = repository.recuperarContrasena(email)
            result.onSuccess {
                _loginState.value = LoginState.ResetEmailSent
            }.onFailure {
                _loginState.value = LoginState.Error(it.message ?: "Error al enviar correo")
            }
        }
    }
}
