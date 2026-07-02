package com.chambita.app.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import com.chambita.app.R
import com.chambita.app.models.Usuario
import com.chambita.app.utils.Distritos
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilTecnicoActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var etEspecialidad: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etExperiencia: EditText
    private lateinit var chipGroupServicios: ChipGroup
    private lateinit var etTarifaMin: EditText
    private lateinit var etTarifaMax: EditText
    private lateinit var spDistrito: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var progresoPerfil: ProgressBar
    private lateinit var txtPorcentaje: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil_tecnico)

        inicializarComponentes()
        configurarSpinner()
        cargarDatos()

        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }

        btnGuardar.setOnClickListener { guardarCambios() }
    }

    private fun inicializarComponentes() {
        etEspecialidad = findViewById(R.id.etEspecialidad)
        etDescripcion = findViewById(R.id.etDescripcion)
        etExperiencia = findViewById(R.id.etExperiencia)
        chipGroupServicios = findViewById(R.id.chipGroupServicios)
        etTarifaMin = findViewById(R.id.etTarifaMin)
        etTarifaMax = findViewById(R.id.etTarifaMax)
        spDistrito = findViewById(R.id.spDistrito)
        btnGuardar = findViewById(R.id.btnGuardarCambios)
        progresoPerfil = findViewById(R.id.progresoPerfil)
        txtPorcentaje = findViewById(R.id.txtPorcentaje)
    }

    private fun configurarSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Distritos.listaLimaCallao)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spDistrito.adapter = adapter
    }

    private fun cargarDatos() {
        if (uid == null) return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val tecnico = doc.toObject(Usuario::class.java)
                    tecnico?.let {
                        findViewById<TextView>(R.id.txtNombre).text = it.nombreCompleto
                        findViewById<TextView>(R.id.txtUbicacion).text = "${it.especialidad} • ${it.distritoActivoHoy}"
                        
                        etEspecialidad.setText(it.especialidad)
                        etDescripcion.setText(it.descripcion)
                        etExperiencia.setText(it.experienciaAnos.toString())
                        etTarifaMin.setText(it.tarifaPorHora.toString())
                        
                        // Seleccionar distrito en el spinner
                        val index = Distritos.listaLimaCallao.indexOf(it.distritoActivoHoy)
                        if (index >= 0) spDistrito.setSelection(index)

                        // Marcar chips
                        it.servicios.forEach { servicio ->
                            when(servicio) {
                                "Instalaciones" -> findViewById<Chip>(R.id.chipInstalaciones).isChecked = true
                                "Reparaciones" -> findViewById<Chip>(R.id.chipReparaciones).isChecked = true
                                "Mantenimiento" -> findViewById<Chip>(R.id.chipMantenimiento).isChecked = true
                            }
                        }

                        actualizarProgreso()
                    }
                }
            }
    }

    private fun guardarCambios() {
        if (uid == null) return

        val especialidad = etEspecialidad.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val experiencia = etExperiencia.text.toString().toIntOrNull() ?: 0
        val tarifaMin = etTarifaMin.text.toString().toDoubleOrNull() ?: 0.0
        val tarifaMax = etTarifaMax.text.toString().toDoubleOrNull() ?: 0.0
        val distrito = spDistrito.selectedItem.toString()

        // --- EXPLICACIÓN PASO A PASO ---
        // 1. Validar que no estén vacíos
        if (tarifaMin <= 0 || tarifaMax <= 0) {
            showToast("Las tarifas deben ser mayores a cero")
            return
        }

        // 2. Validar consistencia (Candado principal)
        if (tarifaMin > tarifaMax) {
            etTarifaMin.error = "No puede ser mayor al máximo"
            etTarifaMin.requestFocus()
            return
        }

        val servicios = mutableListOf<String>()
        if (findViewById<Chip>(R.id.chipInstalaciones).isChecked) servicios.add("Instalaciones")
        if (findViewById<Chip>(R.id.chipReparaciones).isChecked) servicios.add("Reparaciones")
        if (findViewById<Chip>(R.id.chipMantenimiento).isChecked) servicios.add("Mantenimiento")

        val actualizaciones = mapOf(
            "especialidad" to especialidad,
            "descripcion" to descripcion,
            "experienciaAnos" to experiencia,
            "tarifaPorHora" to tarifaMin, // Guardamos la mínima como tarifa base
            "tarifaMaxima" to tarifaMax,   // Guardamos la máxima como campo extra
            "distritoActivoHoy" to distrito,
            "servicios" to servicios
        )

        btnGuardar.isEnabled = false
        db.collection("usuarios").document(uid).update(actualizaciones)
            .addOnSuccessListener {
                showToast("Perfil profesional actualizado")
                actualizarProgreso()
                btnGuardar.isEnabled = true
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
                btnGuardar.isEnabled = true
            }
    }

    private fun actualizarProgreso() {
        var puntos = 0
        if (etEspecialidad.text.isNotEmpty()) puntos += 20
        if (etDescripcion.text.isNotEmpty()) puntos += 20
        if (etExperiencia.text.isNotEmpty()) puntos += 20
        if (etTarifaMin.text.isNotEmpty()) puntos += 20
        
        val servicios = mutableListOf<String>()
        if (findViewById<Chip>(R.id.chipInstalaciones).isChecked) servicios.add("Instalaciones")
        if (findViewById<Chip>(R.id.chipReparaciones).isChecked) servicios.add("Reparaciones")
        if (findViewById<Chip>(R.id.chipMantenimiento).isChecked) servicios.add("Mantenimiento")
        if (servicios.isNotEmpty()) puntos += 20

        progresoPerfil.progress = puntos
        txtPorcentaje.text = "Perfil completado: $puntos%"
    }
}
