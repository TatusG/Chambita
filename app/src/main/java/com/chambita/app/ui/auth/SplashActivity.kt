package com.chambita.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"
    private val viewModel: SplashViewModel by viewModels { AuthViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
        } catch (e: Exception) {
            Log.e(TAG, "Error inflando splash: ${e.message}")
        }

        // Delay de seguridad para ver el logo
        Handler(Looper.getMainLooper()).postDelayed({
            validarSesion()
        }, 2000)
    }

    private fun validarSesion() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            irALogin()
            return
        }

        // Si hay usuario, verificamos la sesión local en Room
        lifecycleScope.launch {
            try {
                viewModel.checkSession()
                // Esperar a que el ViewModel termine la consulta a Room
                viewModel.isReady.filter { it }.first() 
                
                val session = viewModel.sessionState.value
                if (session != null && session.estaActivo) {
                    navegarSegunRol(session.rol)
                } else {
                    irALogin()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en validación: ${e.message}")
                irALogin()
            }
        }
    }

    private fun navegarSegunRol(rol: String) {
        val intent = if (rol == "tecnico") {
            Intent(this, HomeTecnicoActivity::class.java)
        } else {
            Intent(this, HomeClienteActivity::class.java)
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
