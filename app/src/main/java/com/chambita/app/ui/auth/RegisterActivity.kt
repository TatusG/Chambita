package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.chambita.app.R
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import java.util.Date
import android.util.Patterns
import android.view.View

class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etNombre: EditText
    private lateinit var etDni: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var btnCliente: Button
    private lateinit var btnTecnico: Button
    private lateinit var btnCrearCuenta: Button

    private lateinit var tvIniciarSesion: TextView

    private lateinit var progressBar: ProgressBar

    private var rolSeleccionado = "cliente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        Log.d(TAG, "onCreate — RegisterActivity iniciada")

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        inicializarComponentes()
        configurarListeners()
    }

    private fun  inicializarComponentes(){
        etNombre                = findViewById(R.id.etNombre)
        etDni                   = findViewById(R.id.etDni)
        etCorreo                = findViewById(R.id.etCorreo)
        etTelefono              = findViewById(R.id.etTelefono)
        etContrasena            = findViewById(R.id.etContrasena)
        etConfirmarContrasena   = findViewById(R.id.etConfirmarContrasena)
        btnCliente              = findViewById(R.id.btnCliente)
        btnTecnico              = findViewById(R.id.btnTecnico)
        btnCrearCuenta          = findViewById(R.id.btnCrearCuenta)
        tvIniciarSesion         = findViewById(R.id.tvIniciarSesion)
        progressBar             = findViewById(R.id.progressBar)

        Log.d(TAG, "inicializarComponentes - OK")
    }

    private fun configurarListeners(){
        btnCliente.setOnClickListener { seleccionarRol("cliente") }
        btnTecnico.setOnClickListener { seleccionarRol("tecnico") }
        btnCrearCuenta.setOnClickListener { Log.d(TAG,"btnCrearCuenta - click")
            intentarRegistro() }
        tvIniciarSesion.setOnClickListener { finish() }
    }
    private fun seleccionarRol(rol: String) {
        rolSeleccionado = rol
        Log.d(TAG, "Rol seleccionado: $rol")

        if (rol == "cliente") {
            btnCliente.setBackgroundResource(R.drawable.bg_button_primary)
            btnCliente.setTextColor(getColor(R.color.blanco))
            btnTecnico.setBackgroundResource(R.drawable.bg_button_outline)
            btnTecnico.setTextColor(getColor(R.color.chambita_primario))
        } else {
            btnTecnico.setBackgroundResource(R.drawable.bg_button_primary)
            btnTecnico.setTextColor(getColor(R.color.blanco))
            btnCliente.setBackgroundResource(R.drawable.bg_button_outline)
            btnCliente.setTextColor(getColor(R.color.chambita_primario))
        }
    }


    private fun intentarRegistro(){
        val nombre      = etNombre.text.toString().trim()
        val dni         = etDni.text.toString().trim()
        val correo      = etCorreo.text.toString().trim()
        val telefono    = etTelefono.text.toString().trim()
        val contrasena  = etContrasena.text.toString().trim()
        val confirmar   = etConfirmarContrasena.text.toString().trim()

        if (!validarCampos(nombre, dni, correo, telefono, contrasena, confirmar))
            return
        mostrarCarga(true)

        auth.createUserWithEmailAndPassword(correo, contrasena).addOnSuccessListener {
            resultado ->
            val uid = resultado.user?.uid ?: ""
            Log.d(TAG, "Usuario creado en Auth - UID: $uid")
            guardarEnFirestore(uid, nombre, dni, correo, telefono)
        }.addOnFailureListener {
            error -> mostrarCarga(false)
            Log.e(TAG, "Error al crear usuario: ${error.message}")
            Toast.makeText(this, "Error: ${error.message}",
            Toast.LENGTH_LONG).show()
        }
    }


    //Sebas esta parte es para generar los datos del usuario en Firestone ---
    private fun guardarEnFirestore(
        uid: String,
        nombre: String,
        dni: String,
        correo: String,
        telefono: String
    ){
        val usuario = mutableMapOf<String, Any>(
            "uid"           to uid,
            "nombre"        to nombre,
            "dni"           to dni,
            "correo"        to correo,
            "telefono"      to telefono,
            "rol"           to rolSeleccionado,
            "fechaRegistro" to Date()
        )

        // Si es técnico, inicializamos sus campos específicos
        if (rolSeleccionado == "tecnico") {
            usuario["distritos"] = emptyList<String>()
            usuario["disponible"] = false
            usuario["tarifaHora"] = 0.0
        }

        db.collection("usuarios")
            .document(uid)
            .set(usuario)
            .addOnSuccessListener {
                mostrarCarga(false)
                Log.d(TAG,"Usuario guardado en Firestore")
                Toast.makeText(this,"!Cuenta creada exitosamente¡", Toast.LENGTH_SHORT).show()
                irALogin()
            }
            .addOnFailureListener {
                error -> mostrarCarga(false)
                Log.e(TAG,"Error al guardar en Firestore: ${error.message}")
                Toast.makeText(this, "Error al guardar datos: ${error.message}",
                    Toast.LENGTH_LONG).show()
            }
    }

    private fun validarCampos(
        nombre: String,
        dni: String,
        correo: String,
        telefono: String,
        contrasena: String,
        confirmar: String
    ): Boolean {
        if (nombre.isEmpty()) {
            etNombre.error = "Ingrese tu nombre completo"
            etNombre.requestFocus()
            return false
        }
        if (nombre.length < 3) {
            etNombre.error = "Nombre muy corto"
            etNombre.requestFocus()
            return false
        }

        if (dni.isEmpty()) {
            etDni.error = "Ingrese tu DNI"
            etDni.requestFocus()
            return false
        }
        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            etDni.error = "El DNI debe tener exactamente 8 dígitos"
            etDni.requestFocus()
            return false
        }
        if (correo.isEmpty()) {
            etCorreo.error = "Ingresa tu correo"
            etCorreo.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Correo no válido"
            etCorreo.requestFocus()
            return false
        }
        if (telefono.isEmpty()) {
            etTelefono.error = "Ingresa tu teléfono"
            etTelefono.requestFocus()
            return false
        }
        if (telefono.length < 9) {
            etTelefono.error = "Teléfono inválido"
            etTelefono.requestFocus()
            return false
        }
        if (contrasena.isEmpty()) {
            etContrasena.error = "Ingresa una contraseña"
            etContrasena.requestFocus()
            return false
        }
        if (contrasena.length < 6) {
            etContrasena.error = "Mínimo 6 caracteres"
            etContrasena.requestFocus()
            return false
        }
        if (confirmar != contrasena) {
            etConfirmarContrasena.error = "Las contraseñas no coinciden"
            etConfirmarContrasena.requestFocus()
            return false
        }
        return true
    }

    private fun mostrarCarga(mostrar:Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        btnCrearCuenta.isEnabled= !mostrar
    }

    private fun irALogin(){
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy") }

}