package com.chambita.app.ui.auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import com.chambita.app.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NuevaSolicitudActivity : NavActivity() {

    private var fechaSeleccionada: Date? = null
    private var nombreCliente: String = ""
    private var fotoCliente: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_solicitud)

        // Referencias
        val tvVolver = findViewById<TextView>(R.id.tvVolver)
        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        val layoutFecha = findViewById<LinearLayout>(R.id.layoutFecha)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)

        cargarDatosCliente()

        tvVolver?.setOnClickListener { finish() }

        layoutFecha?.setOnClickListener {
            mostrarSelectorFechaHora(tvFechaHora)
        }

        btnConfirmar?.setOnClickListener {
            val descripcion = etDescripcion.text.toString()
            if (descripcion.isEmpty() || fechaSeleccionada == null) {
                showToast("Por favor rellena todos los campos")
            } else {
                enviarSolicitud(descripcion)
            }
        }

        btnCancelar?.setOnClickListener { finish() }
    }

    private fun cargarDatosCliente() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nombreCliente = doc.getString("nombreComplete") ?: ""
                    fotoCliente = doc.getString("fotoPerfil") ?: ""
                }
            }
    }

    private fun mostrarSelectorFechaHora(textView: TextView) {
        val cal = Calendar.getInstance()
        val dpd = DatePickerDialog(this, { _, year, month, day ->
            val tpd = TimePickerDialog(this, { _, hour, min ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, day, hour, min)
                fechaSeleccionada = selectedCal.time
                
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                textView.text = sdf.format(fechaSeleccionada!!)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            tpd.show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        dpd.show()
    }

    private fun enviarSolicitud(descripcion: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val solicitud = hashMapOf(
            "clienteId" to uid,
            "descripcionAveria" to descripcion,
            "direccionServicio" to "Dirección guardada", // Aquí iría lo del Spinner en el futuro
            "distritoServicio" to "Ventanilla",
            "especialidadRequerida" to "General",
            "estado" to "pendiente",
            "fechaCreacion" to Timestamp.now(),
            "fechaServicioProgramado" to Timestamp(fechaSeleccionada!!),
            "fotoCliente" to fotoCliente,
            "montoFinal" to 0,
            "nombreCliente" to nombreCliente,
            "resenaDejada" to false,
            "tecnicoId" to null
        )

        btnConfirmarEnabled(false)
        db.collection("solicitudes").add(solicitud)
            .addOnSuccessListener {
                showToast("Solicitud enviada correctamente")
                finish()
            }
            .addOnFailureListener { e ->
                btnConfirmarEnabled(true)
                showToast("Error: ${e.message}")
            }
    }

    private fun btnConfirmarEnabled(enabled: Boolean) {
        findViewById<Button>(R.id.btnConfirmar)?.isEnabled = enabled
    }
}
