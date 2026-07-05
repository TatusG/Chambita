package com.chambita.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Usuario
import com.chambita.app.utils.Distritos
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class EditarPerfilTecnicoActivity : NavActivity() {

    private val TAG = "EDIT_PERFIL_TECNICO"
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private var tvAvatar: TextView? = null
    private var imgPerfil: ImageView? = null
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
    private var chipGroup: ChipGroup? = null

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                imgPerfil?.visibility = View.VISIBLE
                Glide.with(this).load(selectedImageUri).circleCrop().into(imgPerfil!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_editar_perfil_tecnico)

            barraNavegacion()
            inicializarComponentes()
            configurarSpinner()
            cargarDatos()

            findViewById<View>(R.id.btnVolver)?.setOnClickListener { finish() }
            
            tvAvatar?.setOnClickListener { abrirGaleria() }
            imgPerfil?.setOnClickListener { abrirGaleria() }

            btnGuardar?.setOnClickListener { 
                if (selectedImageUri != null) subirFotoYGuardar()
                else guardarCambios(null)
            }
            
            findViewById<View>(R.id.btnAnadirServicio)?.setOnClickListener {
                mostrarDialogoNuevoServicio()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            showToast("Error al cargar la pantalla")
            finish()
        }
    }

    private fun inicializarComponentes() {
        tvAvatar = findViewById(R.id.tvAvatarInitials)
        imgPerfil = findViewById(R.id.imgPerfilHeader) // Debería añadir este ID al XML o usar el contenedor
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
        chipGroup = findViewById(R.id.chipGroupServicios)
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
                        
                        if (it.fotoPerfil.isNotEmpty()) {
                            imgPerfil?.visibility = View.VISIBLE
                            Glide.with(this).load(it.fotoPerfil).circleCrop().into(imgPerfil!!)
                        }

                        etEspecialidad?.setText(it.especialidad)
                        etDescripcion?.setText(it.descripcion)
                        etExperiencia?.setText(it.experienciaAnos.toString())
                        etTarifaMin?.setText(String.format(Locale.US, "%.2f", it.tarifaPorHora))
                        etTarifaMax?.setText(String.format(Locale.US, "%.2f", it.tarifaMaxima))
                        
                        val index = Distritos.listaLimaCallao.indexOf(it.distritoActivoHoy)
                        if (index >= 0) spDistrito?.setSelection(index)

                        chipGroup?.removeAllViews()
                        it.servicios.forEach { servicio ->
                            agregarChip(servicio)
                        }

                        actualizarProgreso()
                    }
                }
            }
    }

    private fun agregarChip(texto: String) {
        val chip = Chip(this)
        chip.text = texto
        chip.isCheckable = true
        chip.isChecked = true
        chip.chipBackgroundColor = getColorStateList(R.color.figma_blue)
        chip.setTextColor(getColor(R.color.blanco))
        chipGroup?.addView(chip)
    }

    private fun mostrarDialogoNuevoServicio() {
        val input = EditText(this)
        input.hint = "Nombre del servicio"
        AlertDialog.Builder(this)
            .setTitle("Nuevo Servicio")
            .setView(input)
            .setPositiveButton("Añadir") { _, _ ->
                val texto = input.text.toString().trim()
                if (texto.isNotEmpty()) agregarChip(texto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun subirFotoYGuardar() {
        if (uid == null || selectedImageUri == null) return
        btnGuardar?.isEnabled = false
        val ref = storage.reference.child("perfiles/$uid.jpg")
        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    guardarCambios(uri.toString())
                }
            }
            .addOnFailureListener {
                btnGuardar?.isEnabled = true
                showToast("Error al subir foto")
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

    private fun guardarCambios(nuevaFotoUrl: String?) {
        if (uid == null) return

        val especialidad = etEspecialidad?.text.toString().trim()
        val descripcion = etDescripcion?.text.toString().trim()
        val experiencia = etExperiencia?.text.toString().toIntOrNull() ?: 0
        val tarifaMin = etTarifaMin?.text.toString().toDoubleOrNull() ?: 0.0
        val tarifaMax = etTarifaMax?.text.toString().toDoubleOrNull() ?: 0.0
        val distrito = spDistrito?.selectedItem?.toString() ?: ""

        val servicios = mutableListOf<String>()
        for (i in 0 until (chipGroup?.childCount ?: 0)) {
            val chip = chipGroup?.getChildAt(i) as? Chip
            if (chip?.isChecked == true) servicios.add(chip.text.toString())
        }

        val actualizaciones = mutableMapOf<String, Any>(
            "especialidad" to especialidad,
            "descripcion" to descripcion,
            "experienciaAnos" to experiencia,
            "tarifaPorHora" to tarifaMin,
            "tarifaMaxima" to tarifaMax,
            "distritoActivoHoy" to distrito,
            "servicios" to servicios
        )

        if (nuevaFotoUrl != null) actualizaciones["fotoPerfil"] = nuevaFotoUrl

        btnGuardar?.isEnabled = false
        db.collection("usuarios").document(uid).update(actualizaciones)
            .addOnSuccessListener {
                showToast("Perfil actualizado")
                finish()
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
        if ((chipGroup?.childCount ?: 0) > 0) puntos += 20
        progresoPerfil?.progress = puntos
        txtPorcentaje?.text = "Perfil completado: $puntos%"
    }
}
