package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chambita.app.R
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private val viewModel: LoginViewModel by viewModels { AuthViewModelFactory(this) }

    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnIngresar: Button
    private lateinit var btnRegistro: Button
    private lateinit var tvRegistro: TextView
    private lateinit var tvOlvideContrasena: TextView
    private lateinit var pbCargando: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "onCreate - LoginActivity lista")

        inicializarComponentes()
        configurarListeners()
        observarEstado()
    }

    private fun inicializarComponentes() {
        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        btnIngresar = findViewById(R.id.btnIngresar)
        btnRegistro = findViewById(R.id.btnRegistro)
        tvRegistro = findViewById(R.id.tvRegistro)
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena)
        pbCargando = findViewById(R.id.pbCargando)
    }

    private fun configurarListeners() {
        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            if (validarCampos(correo, contrasena)) {
                viewModel.login(correo, contrasena)
            }
        }

        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvOlvideContrasena.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            if (correo.isEmpty()) {
                etCorreo.error = "Ingresa tu correo para recuperar contraseña"
                etCorreo.requestFocus()
            } else {
                viewModel.recuperarContrasena(correo)
            }
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginState.Loading -> mostrarCargando(true)
                        is LoginState.Success -> {
                            mostrarCargando(false)
                            navegarSegunRol(state.rol)
                        }
                        is LoginState.Error -> {
                            mostrarCargando(false)
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        is LoginState.ResetEmailSent -> {
                            mostrarCargando(false)
                            Toast.makeText(this@LoginActivity, "Correo de recuperación enviado", Toast.LENGTH_LONG).show()
                        }
                        is LoginState.Idle -> mostrarCargando(false)
                    }
                }
            }
        }
    }

    private fun navegarSegunRol(rol: String) {
        val intent = if (rol == "cliente") {
            Intent(this, HomeClienteActivity::class.java)
        } else {
            Intent(this, HomeTecnicoActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun validarCampos(correo: String, contrasena: String): Boolean {
        return when {
            correo.isEmpty() -> mostrarError(etCorreo, getString(R.string.error_correo_vacio))
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> mostrarError(etCorreo, getString(R.string.error_correo_invalido))
            contrasena.isEmpty() -> mostrarError(etContrasena, getString(R.string.error_contrasena_vacia))
            contrasena.length < 6 -> mostrarError(etContrasena, "Mínimo 6 caracteres")
            else -> true
        }
    }

    private fun mostrarError(editText: EditText, mensaje: String): Boolean {
        editText.error = mensaje
        editText.requestFocus()
        return false
    }

    private fun mostrarCargando(estaCargando: Boolean) {
        btnIngresar.isEnabled = !estaCargando
        pbCargando.visibility = if (estaCargando) View.VISIBLE else View.GONE
    }
}
