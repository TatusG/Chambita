package com.chambita.app.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.chambita.app.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditarPerfilActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        val btnVolver = findViewById<LinearLayout>(R.id.btnVolver)
        val etFechaNacimiento = findViewById<EditText>(R.id.etFechaNacimiento)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etDni = findViewById<EditText>(R.id.etDni)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)

        cargarDatosActuales(etNombre, etDni, etCorreo, etTelefono, etFechaNacimiento)

        btnVolver?.setOnClickListener { finish() }

        etFechaNacimiento?.setOnClickListener {
            mostrarSelectorFecha(etFechaNacimiento)
        }

        btnGuardar?.setOnClickListener {
            guardarCambios(
                etNombre.text.toString(),
                etDni.text.toString(),
                etCorreo.text.toString(),
                etTelefono.text.toString(),
                etFechaNacimiento.text.toString()
            )
        }
    }

    private fun cargarDatosActuales(nom: EditText, dni: EditText, mail: EditText, tel: EditText, fecha: EditText) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nom.setText(doc.getString("nombreCompleto") ?: "")
                    dni.setText(doc.getString("dni") ?: "")
                    mail.setText(doc.getString("correo") ?: "")
                    tel.setText(doc.getString("telefono") ?: "")
                    
                    // CORRECCIÓN: Manejar el campo fechaNacimiento como Timestamp
                    try {
                        val timestamp = doc.getTimestamp("fechaNacimiento")
                        if (timestamp != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            fecha.setText(sdf.format(timestamp.toDate()))
                        } else {
                            // Si no es un Timestamp, intentamos como String por si acaso
                            fecha.setText(doc.getString("fechaNacimiento") ?: "")
                        }
                    } catch (e: Exception) {
                        Log.e("EditarPerfil", "Error al leer fecha: ${e.message}")
                        fecha.setText("")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditarPerfil", "Error al cargar documento: ${e.message}")
            }
    }

    private fun guardarCambios(nombre: String, dni: String, correo: String, tel: String, fechaTexto: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Convertir el texto a Date para guardarlo como Timestamp
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaDate = try { sdf.parse(fechaTexto) } catch (e: Exception) { null }

        val actualizaciones = hashMapOf(
            "nombreCompleto" to nombre,
            "dni" to dni,
            "correo" to correo,
            "telefono" to tel,
            "fechaNacimiento" to (fechaDate?.let { Timestamp(it) } ?: Timestamp.now())
        )

        findViewById<Button>(R.id.btnGuardar)?.isEnabled = false
        db.collection("usuarios").document(uid).update(actualizaciones as Map<String, Any>)
            .addOnSuccessListener {
                showToast("Perfil actualizado correctamente")
                finish()
            }
            .addOnFailureListener { e ->
                findViewById<Button>(R.id.btnGuardar)?.isEnabled = true
                showToast("Error al actualizar: ${e.message}")
            }
    }

    private fun mostrarSelectorFecha(editText: EditText) {
        val cal = Calendar.getInstance()
        val dpd = DatePickerDialog(this, { _, year, month, day ->
            editText.setText(String.format("%02d/%02d/%d", day, month + 1, year))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        dpd.show()
    }
}
