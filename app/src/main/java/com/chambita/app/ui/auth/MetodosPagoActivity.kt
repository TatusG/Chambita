package com.chambita.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.MetodoPago
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MetodosPagoActivity : NavActivity() {

    private lateinit var adapter: MetodoPagoAdapter
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metodos_pago)

        barraNavegacion()
        inicializarComponentes()
        cargarMetodosPago()
    }

    private fun inicializarComponentes() {
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvMetodosPago)
        adapter = MetodoPagoAdapter(
            emptyList(),
            onSetDefault = { metodo -> cambiarMetodoPredeterminado(metodo.id) },
            onEdit = { metodo -> mostrarDialogoEditarMetodo(metodo) },
            onDelete = { metodo -> eliminarMetodo(metodo.id) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<LinearLayout>(R.id.btnAgregarMetodo)?.setOnClickListener {
            mostrarDialogoAgregarMetodo()
        }
    }

    private fun mostrarDialogoEditarMetodo(metodo: MetodoPago) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_metodo, null)
        val rgTipo = view.findViewById<RadioGroup>(R.id.rgTipoMetodo)
        val etNumero = view.findViewById<EditText>(R.id.etNumeroMetodo)
        val tvLabel = view.findViewById<TextView>(R.id.tvLabelNumero)

        // Pre-cargar datos
        when(metodo.tipo) {
            "Yape" -> rgTipo.check(R.id.rbYape)
            "Plin" -> rgTipo.check(R.id.rbPlin)
            "Efectivo" -> rgTipo.check(R.id.rbEfectivo)
        }
        etNumero.setText(metodo.numeroAsociado)
        if (metodo.tipo == "Efectivo") {
            etNumero.visibility = View.GONE
            tvLabel.visibility = View.GONE
        }

        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbEfectivo) {
                etNumero.visibility = View.GONE
                tvLabel.visibility = View.GONE
            } else {
                etNumero.visibility = View.VISIBLE
                tvLabel.visibility = View.VISIBLE
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Editar Medio de Pago")
            .setView(view)
            .setPositiveButton("Actualizar") { _, _ ->
                val selectedId = rgTipo.checkedRadioButtonId
                val nuevoTipo = when(selectedId) {
                    R.id.rbYape -> "Yape"
                    R.id.rbPlin -> "Plin"
                    else -> "Efectivo"
                }
                val nuevoNumero = etNumero.text.toString().trim()

                if (nuevoTipo != "Efectivo" && nuevoNumero.length < 9) {
                    showToast("Ingresa un número válido")
                } else {
                    actualizarMetodo(metodo.id, nuevoTipo, nuevoNumero)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarMetodo(metodoId: String, tipo: String, numero: String) {
        if (uid == null) return
        val updates = mapOf(
            "tipo" to tipo,
            "numeroAsociado" to if (tipo == "Efectivo") "N/A" else numero
        )
        db.collection("usuarios").document(uid).collection("metodos_pago").document(metodoId)
            .update(updates)
            .addOnSuccessListener {
                showToast("Método actualizado")
                cargarMetodosPago()
            }
    }

    private fun eliminarMetodo(metodoId: String) {
        if (uid == null) return
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar método")
            .setMessage("¿Estás seguro de eliminar este medio de pago?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("usuarios").document(uid).collection("metodos_pago").document(metodoId)
                    .delete()
                    .addOnSuccessListener {
                        showToast("Método eliminado")
                        cargarMetodosPago()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAgregarMetodo() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_metodo, null)
        val rgTipo = view.findViewById<RadioGroup>(R.id.rgTipoMetodo)
        val etNumero = view.findViewById<EditText>(R.id.etNumeroMetodo)
        val tvLabel = view.findViewById<TextView>(R.id.tvLabelNumero)

        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbEfectivo) {
                etNumero.visibility = View.GONE
                tvLabel.visibility = View.GONE
            } else {
                etNumero.visibility = View.VISIBLE
                tvLabel.visibility = View.VISIBLE
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Nuevo Medio de Pago")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val selectedId = rgTipo.checkedRadioButtonId
                val tipo = when(selectedId) {
                    R.id.rbYape -> "Yape"
                    R.id.rbPlin -> "Plin"
                    else -> "Efectivo"
                }
                val numero = etNumero.text.toString().trim()

                if (tipo != "Efectivo" && numero.length < 9) {
                    showToast("Ingresa un número válido")
                } else {
                    guardarNuevoMetodo(tipo, numero)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarNuevoMetodo(tipo: String, numero: String) {
        if (uid == null) return
        val nuevoMetodo = MetodoPago(
            tipo = tipo,
            numeroAsociado = if (tipo == "Efectivo") "N/A" else numero,
            esPredeterminado = false
        )
        
        db.collection("usuarios").document(uid).collection("metodos_pago")
            .add(nuevoMetodo)
            .addOnSuccessListener { 
                showToast("Método agregado con éxito")
                cargarMetodosPago() 
            }
    }

    private fun cargarMetodosPago() {
        if (uid == null) return

        db.collection("usuarios").document(uid).collection("metodos_pago")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.mapNotNull { it.toObject(MetodoPago::class.java).copy(id = it.id) }
                adapter.updateList(lista)
                findViewById<View>(R.id.layoutVacioMetodos)?.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun cambiarMetodoPredeterminado(metodoId: String) {
        if (uid == null) return
        
        db.collection("usuarios").document(uid).collection("metodos_pago").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "esPredeterminado", doc.id == metodoId)
                }
                batch.commit().addOnSuccessListener {
                    showToast("Preferencia de pago actualizada")
                    cargarMetodosPago()
                }
            }
    }
}
