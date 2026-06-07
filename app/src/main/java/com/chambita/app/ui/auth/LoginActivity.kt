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
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

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

        Log.d(TAG, "intentarLogin - validación local OK")

        btnIngresar.isEnabled = false

        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, contrasena).addOnCompleteListener { task ->
            btnIngresar.isEnabled = true

            if (task.isSuccessful) {
                val db = Firebase.firestore
                val user = db.collection("usuarios").document("rol")
                    .get()
                    .addOnSuccessListener { document ->
                        val rol = document.getString("rol")
                        if (rol == "cliente"){
                            val intent = Intent(this, HomeClienteActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }
                        else {
                            Toast.makeText(this, "Bienvenido", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Log.e(TAG, "Error en login: ${task.exception?.message}")
                Toast.makeText(this, "Usuario o Contraseña incorrectos", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validarCampos(correo: String, contrasena: String): Boolean {
        return when {
            correo.isEmpty() ->
                mostrarError(etCorreo, getString(R.string.error_correo_vacio))

            !Patterns.EMAIL_ADDRESS.matcher(correo).matches()->
                mostrarError(etCorreo, getString(R.string.error_correo_invalido))

            contrasena.isEmpty() ->
                mostrarError(etContrasena, getString(R.string.error_contrasena_vacia))

            contrasena.length < 6 ->
                mostrarError(etContrasena, getString(R.string.error_contrasena_caracteres))

            else -> true
        }
    }

    private fun mostrarError(editText: EditText, mensaje: String): Boolean {
        editText.error = mensaje
        editText.requestFocus()
        Log.w(TAG, "Validación fallida: $mensaje")
        return false
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