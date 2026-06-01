package com.chambita.app.ui.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.chambita.app.R
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnIngresar: Button
    private lateinit var btnRegistro: Button
    private lateinit var tvRegistro: TextView
    private lateinit var tvOlvideContrasena: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        Log.d("FIREBASE", "Firebase conectado. Usuario: $firebaseUser")

        Log.d("APP", "Aplicación iniciada correctamente")
        Log.d(TAG, "onCreate - LoginActivity lista")

        inicializarComponentes()
        configurarListener()
    }

    private fun inicializarComponentes() {
        etCorreo            = findViewById(R.id.etCorreo)
        etContrasena        = findViewById(R.id.etContrasena)
        btnIngresar         = findViewById(R.id.btnIngresar)
        btnRegistro         = findViewById(R.id.btnRegistro)
        tvRegistro          = findViewById(R.id.tvRegistro)
        tvOlvideContrasena  = findViewById(R.id.tvOlvideContrasena)
        Log.d(TAG, "inicializarComponentes — OK")
    }

    private fun configurarListener() {
        btnIngresar.setOnClickListener {
            Log.d(TAG, "btnIngresar — click")
            intentarLogin()
        }
        btnRegistro.setOnClickListener {
            Log.d(TAG, "btnRegistro — click")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        tvRegistro.setOnClickListener {
            Log.d(TAG, "tvRegistro — click")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        tvOlvideContrasena.setOnClickListener {
            Log.d(TAG, "tvOlvideContrasena — click")
            Toast.makeText(this, "Recuperación próximamente 🚧", Toast.LENGTH_SHORT).show()
        }
    }

    private fun intentarLogin() {
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString().trim()

        if (!validarCampos(correo, contrasena)) return

        Log.d(TAG, "intentarLogin - validación OK")
        Toast.makeText(this, "!Bienvenido!", Toast.LENGTH_LONG).show()
    }

    private fun validarCampos(correo: String, contrasena: String): Boolean {
        if (correo.isEmpty()) {
            etCorreo.error = getString(R.string.error_correo_vacio)
            etCorreo.requestFocus()
            Log.w(TAG, "validarCampos - correo vacío")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = getString(R.string.error_correo_invalido)
            etCorreo.requestFocus()
            Log.w(TAG, "validarCampos - correo inválido")
            return false
        }

        if (contrasena.isEmpty()) {
            etContrasena.error = getString(R.string.error_contrasena_vacia)
            etContrasena.requestFocus()
            Log.w(TAG, "validarCampos - contraseña vacía")
            return false
        }

        if (contrasena.length < 6) {
            etContrasena.error = "Mínimo 6 caracteres"
            etContrasena.requestFocus()
            Log.w(TAG, "validarCampos - contraseña corta")
            return false
        }
        return true
    }

    override fun onStart() {
        super.onStart(); Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume(); Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause(); Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop(); Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy(); Log.d(TAG, "onDestroy")
    }

}