package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.*
import com.chambita.app.R
import com.chambita.app.models.Usuario
import com.chambita.app.utils.Distritos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CoberturaLaboralActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    
    private lateinit var spinners: List<Spinner>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobertura_laboral)

        barraNavegacion()
        inicializarSpinners()
        cargarCoberturaActual()

        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGuardarCobertura).setOnClickListener { guardarCobertura() }
    }

    private fun inicializarSpinners() {
        spinners = listOf(
            findViewById(R.id.spDistrito1),
            findViewById(R.id.spDistrito2),
            findViewById(R.id.spDistrito3),
            findViewById(R.id.spDistrito4),
            findViewById(R.id.spDistrito5)
        )

        val listaConVacio = listOf("Ninguno") + Distritos.listaLimaCallao
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaConVacio)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinners.forEach { it.adapter = adapter }
    }

    private fun cargarCoberturaActual() {
        if (uid == null) return
        db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
            val tecnico = doc.toObject(Usuario::class.java)
            tecnico?.distritos?.forEachIndexed { index, distrito ->
                if (index < spinners.size) {
                    val pos = (spinners[index].adapter as ArrayAdapter<String>).getPosition(distrito)
                    if (pos >= 0) spinners[index].setSelection(pos)
                }
            }
        }
    }

    private fun guardarCobertura() {
        if (uid == null) return

        val distritosSeleccionados = spinners.map { it.selectedItem.toString() }
            .filter { it != "Ninguno" }
            .distinct()

        if (distritosSeleccionados.isEmpty()) {
            showToast("Selecciona al menos un distrito")
            return
        }

        db.collection("usuarios").document(uid)
            .update("distritos", distritosSeleccionados)
            .addOnSuccessListener {
                showToast("Cobertura actualizada con éxito")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Error al guardar: ${e.message}")
            }
    }
}
