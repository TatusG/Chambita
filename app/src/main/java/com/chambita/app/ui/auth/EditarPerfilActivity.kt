package com.chambita.app.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.utils.Distritos
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class EditarPerfilActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var etNombre: EditText
    private lateinit var etDni: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var spDistrito: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var imgPerfil: ImageView
    private lateinit var tvAvatar: TextView
    private lateinit var tvCambiarFoto: TextView

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                imgPerfil.visibility = View.VISIBLE
                Glide.with(this).load(selectedImageUri).circleCrop().into(imgPerfil)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        inicializarComponentes()
        configurarDistritos()
        cargarDatosActuales()

        findViewById<LinearLayout>(R.id.btnVolver)?.setOnClickListener { finish() }

        etFechaNacimiento.setOnClickListener { mostrarSelectorFecha(etFechaNacimiento) }

        tvCambiarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        btnGuardar.setOnClickListener {
            if (selectedImageUri != null) {
                subirImagenYGuardar()
            } else {
                guardarCambios(null)
            }
        }
    }

    private fun inicializarComponentes() {
        etNombre = findViewById(R.id.etNombre)
        etDni = findViewById(R.id.etDni)
        etCorreo = findViewById(R.id.etCorreo)
        etTelefono = findViewById(R.id.etTelefono)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        spDistrito = findViewById(R.id.spDistrito)
        btnGuardar = findViewById(R.id.btnGuardar)
        imgPerfil = findViewById(R.id.imgPerfil)
        tvAvatar = findViewById(R.id.tvAvatar)
        tvCambiarFoto = findViewById(R.id.tvCambiarFoto)
    }

    private fun configurarDistritos() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Distritos.listaLimaCallao)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spDistrito.adapter = adapter
    }

    private fun cargarDatosActuales() {
        if (uid == null) return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombreCompleto") ?: ""
                    etNombre.setText(nombre)
                    etDni.setText(doc.getString("dni") ?: "")
                    etCorreo.setText(doc.getString("correo") ?: "")
                    etTelefono.setText(doc.getString("telefono") ?: "")
                    
                    tvAvatar.text = if (nombre.isNotEmpty()) nombre.take(1).uppercase() else ""

                    val fotoUrl = doc.getString("fotoPerfil")
                    if (!fotoUrl.isNullOrEmpty()) {
                        imgPerfil.visibility = View.VISIBLE
                        Glide.with(this).load(fotoUrl).circleCrop().into(imgPerfil)
                    }

                    val distrito = doc.getString("distritoResidencia") ?: ""
                    val index = Distritos.listaLimaCallao.indexOf(distrito)
                    if (index >= 0) spDistrito.setSelection(index)

                    try {
                        val timestamp = doc.getTimestamp("fechaNacimiento")
                        if (timestamp != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            etFechaNacimiento.setText(sdf.format(timestamp.toDate()))
                        }
                    } catch (e: Exception) {
                        Log.e("EditarPerfil", "Error al leer fecha: ${e.message}")
                    }
                }
            }
    }

    private fun subirImagenYGuardar() {
        if (uid == null || selectedImageUri == null) return

        btnGuardar.isEnabled = false
        val ref = storage.reference.child("perfiles/$uid.jpg")
        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    guardarCambios(uri.toString())
                }
            }
            .addOnFailureListener {
                btnGuardar.isEnabled = true
                showToast("Error al subir imagen: ${it.message}")
            }
    }

    private fun guardarCambios(nuevaFotoUrl: String?) {
        if (uid == null) return

        val nombre = etNombre.text.toString().trim()
        val dni = etDni.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val tel = etTelefono.text.toString().trim()
        val fechaTexto = etFechaNacimiento.text.toString().trim()
        val distrito = spDistrito.selectedItem.toString()

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaDate = try { sdf.parse(fechaTexto) } catch (e: Exception) { null }

        val actualizaciones = mutableMapOf<String, Any>(
            "nombreCompleto" to nombre,
            "dni" to dni,
            "correo" to correo,
            "telefono" to tel,
            "distritoResidencia" to distrito,
            "fechaNacimiento" to (fechaDate?.let { Timestamp(it) } ?: Timestamp.now())
        )

        if (nuevaFotoUrl != null) {
            actualizaciones["fotoPerfil"] = nuevaFotoUrl
        }

        btnGuardar.isEnabled = false
        db.collection("usuarios").document(uid).update(actualizaciones)
            .addOnSuccessListener {
                showToast("Perfil actualizado correctamente")
                finish()
            }
            .addOnFailureListener { e ->
                btnGuardar.isEnabled = true
                showToast("Error al actualizar: ${e.message}")
            }
    }

    private fun mostrarSelectorFecha(editText: EditText) {
        val cal = Calendar.getInstance()
        val dpd = DatePickerDialog(this, { _, year, month, day ->
            editText.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        dpd.show()
    }
}
