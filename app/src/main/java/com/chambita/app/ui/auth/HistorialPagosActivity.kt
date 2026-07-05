package com.chambita.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Pago
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class HistorialPagosActivity : NavActivity() {

    private lateinit var adapter: PagoAdapter
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_pagos)

        barraNavegacion()
        inicializarComponentes()
        cargarHistorialPagos()
    }

    private fun inicializarComponentes() {
        findViewById<View>(R.id.btnVolver)?.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvHistorialPagos)
        adapter = PagoAdapter(emptyList())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun cargarHistorialPagos() {
        if (uid == null) return

        // Simplificamos la consulta eliminando el orderBy para evitar el error FAILED_PRECONDITION (falta de índice)
        db.collection("pagos")
            .whereEqualTo("clienteId", uid)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(Pago::class.java)
                
                // Ordenamos localmente por fecha descendente
                val listaOrdenada = lista.sortedByDescending { it.fechaRegistro }
                
                adapter.updateList(listaOrdenada)
                
                var total = 0.0
                listaOrdenada.forEach { total += it.monto }
                findViewById<TextView>(R.id.tvTotalGastado)?.text = String.format(Locale.US, "S/ %.2f", total)
                
                findViewById<TextView>(R.id.tvResumenMes)?.text = "${listaOrdenada.size} servicios • Total"
            }
            .addOnFailureListener { e ->
                showToast("Error al cargar pagos: ${e.message}")
            }
    }
}
