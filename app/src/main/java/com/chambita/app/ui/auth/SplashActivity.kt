package com.chambita.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Log.d(TAG, "onCreate — SplashActivity iniciada")

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            verificarSesion()
        }, 2500)
    }

    private fun verificarSesion() {
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            Log.d(TAG, "Sesión activa: ${usuarioActual.email}")
            irALogin()
        } else {
            Log.d(TAG, "Sin sesión — redirigiendo a Login")
            irALogin()
        }
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}