package com.chambita.app.ui.auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
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
    private var categoriaSeleccionada: String = "Instalación" // Por defecto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_solicitud)

        val tvVolver = findViewById<TextView>(R.id.tvVolver)
        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        val layoutFecha = findViewById<LinearLayout>(R.id.layoutFecha)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        
        // Botones de Categoría
        val btnInstalacion = findViewById<Button>(R.id.btnInstalacion)
        val btnReparacion = findViewById<Button>(R.id.btnReparacion)
        val btnMantenimiento = findViewById<Button>(R.id.btnMantenimiento)

        cargarDatosCliente()

        // Lógica de Selección de Categoría
        val botones = listOf(btnInstalacion, btnReparacion, btnMantenimiento)
        botones.forEach { boton ->
            boton?.setOnClickListener {
                seleccionarBoton(boton, botones)
                categoriaSeleccionada = boton.text.toString()
            }
        }

        tvVolver?.setOnClickListener { finish() }
        layoutFecha?.setOnClickListener { mostrarSelectorFechaHora(tvFechaHora) }

        btnConfirmar?.setOnClickListener {
            val descripcion = etDescripcion.text.toString()
            if (descripcion.isEmpty() || fechaSeleccionada == null) {
                showToast("Por favor, describe el problema y elige una fecha")
            } else {
                enviarSolicitud(descripcion)
            }
        }
    }

    private fun seleccionarBoton(seleccionado: Button, todos: List<Button?>) {
        todos.forEach {
            it?.setBackgroundResource(R.drawable.bg_button_outline)
            it?.setTextColor(Color.parseColor("#4B5563"))
        }
        seleccionado.setBackgroundResource(R.drawable.bg_button_primary)
        seleccionado.setTextColor(Color.WHITE)
    }

    private fun cargarDatosCliente() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nombreCliente = doc.getString("nombreCompleto") ?: ""
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
            "tecnicoId" to null, // Inicialmente nulo según especificación
            "descripcionAveria" to descripcion,
            "especialidadRequerida" to categoriaSeleccionada,
            "fechaCreacion" to Timestamp.now(),
            "fechaServicioProgramado" to Timestamp(fechaSeleccionada!!),
            "direccionServicio" to "Av. Principal 123", // Valor de prueba
            "estado" to "pendiente",
            "montoFinal" to 0.0,
            "resenaDejada" to false,
            "nombreCliente" to nombreCliente,
            "fotoCliente" to fotoCliente
        )

        findViewById<Button>(R.id.btnConfirmar)?.isEnabled = false
        db.collection("solicitudes").add(solicitud)
            .addOnSuccessListener {
                showToast("¡Solicitud enviada! Esperando técnico...")
                finish()
            }
            .addOnFailureListener { e ->
                findViewById<Button>(R.id.btnConfirmar)?.isEnabled = true
                showToast("Error: ${e.message}")
            }
    }
}
