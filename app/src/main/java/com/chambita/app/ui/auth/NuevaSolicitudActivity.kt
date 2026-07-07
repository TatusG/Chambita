package com.chambita.app.ui.auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.chambita.app.R
import com.chambita.app.models.Direccion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NuevaSolicitudActivity : NavActivity() {

    private val TAG = "NUEVA_SOLICITUD"
    private var fechaSeleccionada: Date? = null
    private var nombreCliente: String = ""
    private var fotoCliente: String = ""
    private var categoriaSeleccionada: String = "Instalación"
    private var tecnicoId: String? = null
    
    private lateinit var spDireccion: Spinner
    private var listaDireccionesFull: List<Direccion> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_solicitud)

        tecnicoId = intent.getStringExtra("tecnicoId")
        val nombreTecnico = intent.getStringExtra("nombreTecnico")
        val tarifa = intent.getDoubleExtra("tarifa", 0.0)

        inicializarUI(nombreTecnico, tarifa)
        cargarDatosCliente()
        cargarDirecciones()
    }

    private fun inicializarUI(nombreTecnico: String?, tarifa: Double) {
        val tvResumen = findViewById<TextView>(R.id.tvResumenTecnico)
        val tvTarifa = findViewById<TextView>(R.id.tvTarifa)
        val tvTecnicoLabel = findViewById<TextView>(R.id.tvTecnico)
        
        if (tecnicoId != null) {
            tvResumen.text = nombreTecnico ?: "Profesional"
            tvTarifa.text = String.format(Locale.US, "S/. %.2f", tarifa)
            tvTecnicoLabel.text = "Contratando a: ${nombreTecnico ?: "profesional"}"
        } else {
            tvResumen.text = "Publicación abierta"
            tvTarifa.text = "A convenir"
            tvTecnicoLabel.text = "Publicando para técnicos cercanos"
        }

        spDireccion = findViewById(R.id.spDireccion)
        val etDesc = findViewById<EditText>(R.id.etDescripcion)
        val tvFecha = findViewById<TextView>(R.id.tvFechaHora)
        
        findViewById<LinearLayout>(R.id.layoutFecha)?.setOnClickListener { mostrarSelectorFechaHora(tvFecha) }
        findViewById<TextView>(R.id.tvVolver)?.setOnClickListener { finish() }

        // Categorías
        val botones = listOf(
            findViewById<Button>(R.id.btnInstalacion),
            findViewById<Button>(R.id.btnReparacion),
            findViewById<Button>(R.id.btnMantenimiento)
        )
        botones.forEach { btn ->
            btn?.setOnClickListener {
                seleccionarBoton(btn, botones)
                categoriaSeleccionada = btn.text.toString()
            }
        }

        findViewById<Button>(R.id.btnConfirmar)?.setOnClickListener {
            val desc = etDesc.text.toString().trim()
            if (desc.isEmpty() || fechaSeleccionada == null) {
                showToast("Completa la descripción y la fecha")
            } else {
                enviarSolicitud(desc)
            }
        }

        findViewById<Button>(R.id.btnCancelar)?.setOnClickListener { finish() }
    }

    private fun cargarDirecciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .collection("direcciones")
            .get()
            .addOnSuccessListener { snapshot ->
                listaDireccionesFull = snapshot.toObjects(Direccion::class.java)
                
                val visualList = if (listaDireccionesFull.isEmpty()) {
                    listOf("Agrega una dirección en tu perfil")
                } else {
                    listaDireccionesFull.map { "${it.alias}: ${it.direccion} (${it.distrito})" }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, visualList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spDireccion.adapter = adapter
            }
    }

    private fun enviarSolicitud(descripcion: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val index = spDireccion.selectedItemPosition
        
        if (listaDireccionesFull.isEmpty()) {
            showToast("Debes tener una dirección guardada")
            return
        }

        val direccionElegida = listaDireccionesFull[index]
        val db = FirebaseFirestore.getInstance()

        val solicitud = hashMapOf(
            "clienteId" to uid,
            "tecnicoId" to tecnicoId,
            "descripcionAveria" to descripcion,
            "especialidadRequerida" to categoriaSeleccionada,
            "fechaCreacion" to Timestamp.now(),
            "fechaServicioProgramado" to Timestamp(fechaSeleccionada!!),
            "direccionServicio" to direccionElegida.direccion,
            "distritoServicio" to direccionElegida.distrito, // ✅ FIX: Ahora sí guardamos el distrito
            "estado" to "pendiente",
            "montoFinal" to 0.0,
            "resenaDejada" to false,
            "nombreCliente" to nombreCliente,
            "fotoCliente" to fotoCliente
        )

        findViewById<Button>(R.id.btnConfirmar)?.isEnabled = false
        db.collection("solicitudes").add(solicitud)
            .addOnSuccessListener {
                showToast("¡Solicitud enviada!")
                startActivity(Intent(this, MisSolicitudesActivity::class.java))
                finish()
            }
            .addOnFailureListener { 
                findViewById<Button>(R.id.btnConfirmar)?.isEnabled = true
                showToast("Error al enviar")
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
        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                val sel = Calendar.getInstance().apply { set(y, m, d, h, min) }
                fechaSeleccionada = sel.time
                textView.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(fechaSeleccionada!!)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}
