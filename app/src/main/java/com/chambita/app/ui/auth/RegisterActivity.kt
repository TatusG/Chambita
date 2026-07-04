package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

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

    private fun inicializarComponentes(){
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
        btnCrearCuenta.setOnClickListener { 
            Log.d(TAG,"btnCrearCuenta - click")
            intentarRegistro() 
        }
        tvIniciarSesion.setOnClickListener { finish() }

        setupPasswordToggle(etContrasena)
        setupPasswordToggle(etConfirmarContrasena)
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText) {
        editText.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editText.right - editText.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - editText.paddingEnd)) {
                    togglePasswordVisibility(editText)
                    editText.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private var isPasswordVisible = false

    private fun togglePasswordVisibility(editText: EditText) {
        if (isPasswordVisible) {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
        } else {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0)
        }
        isPasswordVisible = !isPasswordVisible
        editText.setSelection(editText.text.length)
    }

    private fun seleccionarRol(rol: String) {
        rolSeleccionado = rol
        Log.d(TAG, "Rol seleccionado: $rol")

        val colorBlanco = getColor(R.color.blanco)
        val colorTextoPrincipal = getColor(R.color.chambita_texto_principal)

        if (rol == "cliente") {
            btnCliente.setBackgroundResource(R.drawable.bg_button_primary)
            btnCliente.setTextColor(colorBlanco)
            btnCliente.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_person, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(btnCliente, android.content.res.ColorStateList.valueOf(colorBlanco))

            btnTecnico.setBackgroundResource(android.R.color.transparent)
            btnTecnico.setTextColor(colorTextoPrincipal)
            btnTecnico.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tools, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(btnTecnico, android.content.res.ColorStateList.valueOf(colorTextoPrincipal))
        } else {
            btnTecnico.setBackgroundResource(R.drawable.bg_button_primary)
            btnTecnico.setTextColor(colorBlanco)
            btnTecnico.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tools, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(btnTecnico, android.content.res.ColorStateList.valueOf(colorBlanco))

            btnCliente.setBackgroundResource(android.R.color.transparent)
            btnCliente.setTextColor(colorTextoPrincipal)
            btnCliente.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_person, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(btnCliente, android.content.res.ColorStateList.valueOf(colorTextoPrincipal))
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

    private fun guardarEnFirestore(
        uid: String,
        nombre: String,
        dni: String,
        correo: String,
        telefono: String
    ){
        val usuario = mutableMapOf<String, Any>(
            "uid"           to uid,
            "nombreCompleto" to nombre,
            "dni"           to dni,
            "correo"        to correo,
            "telefono"      to telefono,
            "rol"           to rolSeleccionado,
            "fechaRegistro" to Date()
        )

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
        nombre: String, dni: String, correo: String,
        telefono: String, contrasena: String, confirmar: String
    ): Boolean {
        return when {
            nombre.isEmpty() -> mostrarError(etNombre, "Ingrese su nombre")
            nombre.length < 3 -> mostrarError(etNombre, "Nombre es muy corto")
            dni.length != 8 -> mostrarError(etDni, "DNI debe tener 8 dígitos")
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> mostrarError(etCorreo, "Correo inválido")
            telefono.length < 9 -> mostrarError(etTelefono, "Teléfono inválido")
            contrasena.length < 6 -> mostrarError(etContrasena, "Contraseña corta")
            confirmar != contrasena -> mostrarError(etConfirmarContrasena, "Las contraseñas no coinciden")
            else -> true
        }
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

    private fun mostrarError(editText: EditText, mensaje: String): Boolean {
        editText.error = mensaje
        editText.requestFocus()
        Log.w(TAG, "Validación fallida: $mensaje")
        return false
    }

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy") }

}
