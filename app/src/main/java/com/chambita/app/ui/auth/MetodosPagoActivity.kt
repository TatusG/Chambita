package com.chambita.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
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
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvMetodosPago)
        adapter = MetodoPagoAdapter(emptyList()) { metodo ->
            cambiarMetodoPredeterminado(metodo.id)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<LinearLayout>(R.id.btnAgregarMetodo)?.setOnClickListener {
            // MOCK: En una app real abriríamos un formulario
            showToast("Añadiendo Yape de prueba...")
            agregarMetodoPrueba()
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

    private fun agregarMetodoPrueba() {
        if (uid == null) return
        val nuevoMetodo = MetodoPago(
            tipo = "Yape",
            numeroAsociado = "9** *** ***"
        )
        db.collection("usuarios").document(uid).collection("metodos_pago")
            .add(nuevoMetodo)
            .addOnSuccessListener { cargarMetodosPago() }
    }

    private fun cambiarMetodoPredeterminado(metodoId: String) {
        if (uid == null) return
        
        // 1. Obtener todos para resetear
        db.collection("usuarios").document(uid).collection("metodos_pago").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "esPredeterminado", doc.id == metodoId)
                }
                batch.commit().addOnSuccessListener {
                    showToast("Método predeterminado actualizado")
                    cargarMetodosPago()
                }
            }
    }
}
