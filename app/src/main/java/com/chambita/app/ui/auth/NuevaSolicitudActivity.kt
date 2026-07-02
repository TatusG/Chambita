package com.chambita.app.ui.auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
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
    private var tecnicoId: String? = null
    
    private lateinit var spDireccion: Spinner
    private var direccionesList: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_solicitud)

        tecnicoId = intent.getStringExtra("tecnicoId")
        val nombreTecnico = intent.getStringExtra("nombreTecnico")
        val tarifa = intent.getDoubleExtra("tarifa", 0.0)

        if (tecnicoId != null) {
            findViewById<TextView>(R.id.tvResumenTecnico).text = nombreTecnico ?: "Profesional"
            findViewById<TextView>(R.id.tvTarifa).text = String.format(Locale.getDefault(), "S/. %.2f", tarifa)
            findViewById<TextView>(R.id.tvTecnico).text = "Contratando a: ${nombreTecnico ?: "profesional"}"
        } else {
            findViewById<TextView>(R.id.tvResumenTecnico).text = "Publicación abierta"
            findViewById<TextView>(R.id.tvTarifa).text = "A convenir"
            findViewById<TextView>(R.id.tvTecnico).text = "Publicando para técnicos cercanos"
        }

        val tvVolver = findViewById<TextView>(R.id.tvVolver)
        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        val layoutFecha = findViewById<LinearLayout>(R.id.layoutFecha)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        spDireccion = findViewById(R.id.spDireccion)
        
        // Botones de Categoría
        val btnInstalacion = findViewById<Button>(R.id.btnInstalacion)
        val btnReparacion = findViewById<Button>(R.id.btnReparacion)
        val btnMantenimiento = findViewById<Button>(R.id.btnMantenimiento)

        cargarDatosCliente()
        cargarDirecciones()

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

    private fun cargarDirecciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .collection("direcciones")
            .get()
            .addOnSuccessListener { snapshot ->
                direccionesList = snapshot.documents.map { it.getString("direccion") ?: "" }.filter { it.isNotEmpty() }
                if (direccionesList.isEmpty()) {
                    direccionesList = listOf("No tienes direcciones guardadas")
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, direccionesList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spDireccion.adapter = adapter
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
        val direccion = spDireccion.selectedItem?.toString() ?: "Sin dirección"

        if (direccion == "No tienes direcciones guardadas") {
            showToast("Por favor, agrega una dirección en tu perfil primero")
            return
        }

        val solicitud = hashMapOf(
            "clienteId" to uid,
            "tecnicoId" to tecnicoId,
            "descripcionAveria" to descripcion,
            "especialidadRequerida" to categoriaSeleccionada,
            "fechaCreacion" to Timestamp.now(),
            "fechaServicioProgramado" to Timestamp(fechaSeleccionada!!),
            "direccionServicio" to direccion,
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
                // Navegar directamente a Mis Solicitudes para ver el estado
                val intent = Intent(this, MisSolicitudesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                findViewById<Button>(R.id.btnConfirmar)?.isEnabled = true
                showToast("Error: ${e.message}")
            }
    }
}
