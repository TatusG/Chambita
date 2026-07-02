package com.chambita.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.chambita.app.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"
    private val viewModel: SplashViewModel by viewModels { AuthViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Log.d(TAG, "onCreate — SplashActivity iniciada")

        observarEstado()
        
        // Simular tiempo de carga del splash (2.5 seg)
        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.checkSession()
        }, 2500)
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isReady.collect { ready ->
                    if (ready) {
                        val session = viewModel.sessionState.value
                        if (session != null && session.estaActivo) {
                            Log.d(TAG, "Sesión local activa: ${session.correo} - Rol: ${session.rol}")
                            navegarSegunRol(session.rol)
                        } else {
                            Log.d(TAG, "Sin sesión activa — redirigiendo a Login")
                            irALogin()
                        }
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
        startActivity(intent)
        finish()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
