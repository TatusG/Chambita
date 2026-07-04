package com.chambita.app.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.chambita.app.R
import com.chambita.app.models.Usuario
import com.chambita.app.utils.Distritos
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class EditarPerfilTecnicoActivity : NavActivity() {

    private val TAG = "EDIT_PERFIL_TECNICO"
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private var tvAvatar: TextView? = null
    private var txtNombreHeader: TextView? = null
    private var txtUbicacionHeader: TextView? = null
    private var etEspecialidad: EditText? = null
    private var etDescripcion: EditText? = null
    private var etExperiencia: EditText? = null
    private var etTarifaMin: EditText? = null
    private var etTarifaMax: EditText? = null
    private var spDistrito: Spinner? = null
    private var btnGuardar: Button? = null
    private var progresoPerfil: ProgressBar? = null
    private var txtPorcentaje: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_editar_perfil_tecnico)

            barraNavegacion()
            inicializarComponentes()
            configurarSpinner()
            cargarDatos()

            findViewById<View>(R.id.btnVolver)?.setOnClickListener { finish() }
            btnGuardar?.setOnClickListener { guardarCambios() }
            
            findViewById<View>(R.id.btnAnadirServicio)?.setOnClickListener {
                showToast("Función para añadir nuevos servicios próximamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            showToast("Error al cargar la pantalla")
            finish()
        }
    }

    private fun inicializarComponentes() {
        tvAvatar = findViewById(R.id.tvAvatarInitials)
        txtNombreHeader = findViewById(R.id.txtNombre)
        txtUbicacionHeader = findViewById(R.id.txtUbicacion)
        etEspecialidad = findViewById(R.id.etEspecialidad)
        etDescripcion = findViewById(R.id.etDescripcion)
        etExperiencia = findViewById(R.id.etExperiencia)
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
        spDistrito?.adapter = adapter
    }

    private fun cargarDatos() {
        if (uid == null) return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val tecnico = doc.toObject(Usuario::class.java)
                    tecnico?.let {
                        txtNombreHeader?.text = it.nombreCompleto
                        txtUbicacionHeader?.text = "${it.especialidad} • ${it.distritoActivoHoy}"
                        tvAvatar?.text = obtenerIniciales(it.nombreCompleto)
                        
                        etEspecialidad?.setText(it.especialidad)
                        etDescripcion?.setText(it.descripcion)
                        etExperiencia?.setText(it.experienciaAnos.toString())
                        etTarifaMin?.setText(it.tarifaPorHora.toInt().toString())
                        etTarifaMax?.setText(it.tarifaMaxima.toInt().toString())
                        
                        val index = Distritos.listaLimaCallao.indexOf(it.distritoActivoHoy)
                        if (index >= 0) spDistrito?.setSelection(index)

                        it.servicios.forEach { servicio ->
                            when(servicio) {
                                "Instalaciones" -> findViewById<Chip>(R.id.chipInstalaciones)?.isChecked = true
                                "Reparaciones" -> findViewById<Chip>(R.id.chipReparaciones)?.isChecked = true
                                "Mantenimiento", "Mtto." -> findViewById<Chip>(R.id.chipMantenimiento)?.isChecked = true
                            }
                        }

                        actualizarProgreso()
                    }
                }
            }
    }

    private fun obtenerIniciales(nombre: String): String {
        val partes = nombre.trim().split("\\s+".toRegex())
        return if (partes.size >= 2) {
            "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        } else {
            nombre.take(2).uppercase()
        }
    }

    private fun guardarCambios() {
        if (uid == null) return

        val especialidad = etEspecialidad?.text.toString().trim()
        val descripcion = etDescripcion?.text.toString().trim()
        val experiencia = etExperiencia?.text.toString().replace(" años", "").trim().toIntOrNull() ?: 0
        val tarifaMin = etTarifaMin?.text.toString().toDoubleOrNull() ?: 0.0
        val tarifaMax = etTarifaMax?.text.toString().toDoubleOrNull() ?: 0.0
        val distrito = spDistrito?.selectedItem?.toString() ?: ""

        if (tarifaMin <= 0 || tarifaMax <= 0) {
            showToast("Las tarifas deben ser mayores a cero")
            return
        }

        if (tarifaMin > tarifaMax) {
            etTarifaMin?.error = "No puede ser mayor al máximo"
            etTarifaMin?.requestFocus()
            return
        }

        val servicios = mutableListOf<String>()
        if (findViewById<Chip>(R.id.chipInstalaciones)?.isChecked == true) servicios.add("Instalaciones")
        if (findViewById<Chip>(R.id.chipReparaciones)?.isChecked == true) servicios.add("Reparaciones")
        if (findViewById<Chip>(R.id.chipMantenimiento)?.isChecked == true) servicios.add("Mtto.")

        val actualizaciones = mapOf(
            "especialidad" to especialidad,
            "descripcion" to descripcion,
            "experienciaAnos" to experiencia,
            "tarifaPorHora" to tarifaMin,
            "tarifaMaxima" to tarifaMax,
            "distritoActivoHoy" to distrito,
            "servicios" to servicios
        )

        btnGuardar?.isEnabled = false
        db.collection("usuarios").document(uid).update(actualizaciones)
            .addOnSuccessListener {
                showToast("Perfil profesional actualizado")
                actualizarProgreso()
                btnGuardar?.isEnabled = true
                // Actualizar cabecera visual
                txtNombreHeader?.text = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                txtUbicacionHeader?.text = "$especialidad • $distrito"
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
                btnGuardar?.isEnabled = true
            }
    }

    private fun actualizarProgreso() {
        var puntos = 0
        if (etEspecialidad?.text?.isNotEmpty() == true) puntos += 20
        if (etDescripcion?.text?.isNotEmpty() == true) puntos += 20
        if (etExperiencia?.text?.isNotEmpty() == true) puntos += 20
        if (etTarifaMin?.text?.isNotEmpty() == true) puntos += 20
        
        val servicios = mutableListOf<String>()
        if (findViewById<Chip>(R.id.chipInstalaciones)?.isChecked == true) servicios.add("Instalaciones")
        if (findViewById<Chip>(R.id.chipReparaciones)?.isChecked == true) servicios.add("Reparaciones")
        if (findViewById<Chip>(R.id.chipMantenimiento)?.isChecked == true) servicios.add("Mtto.")
        if (servicios.isNotEmpty()) puntos += 20

        progresoPerfil?.progress = puntos
        txtPorcentaje?.text = "Perfil completado: $puntos%"
    }
}
