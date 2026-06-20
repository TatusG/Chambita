package com.chambita.app.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialPagosActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_pagos)

        barraNavegacion()

        findViewById<View>(R.id.btnVolver)?.setOnClickListener { finish() }

        cargarHistorialPagos()
    }

    private fun cargarHistorialPagos() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Siguiendo tu especificación: Colección pagos filtrada por clienteId
        db.collection("pagos")
            .whereEqualTo("clientId", uid) // Nota: Revisa si es clientId o clienteId en tu base de datos real
            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // Si tienes un layout de "vacío", lo muestras aquí
                    showToast("No tienes historial de pagos")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error al cargar pagos: ${e.message}")
            }
    }
}
