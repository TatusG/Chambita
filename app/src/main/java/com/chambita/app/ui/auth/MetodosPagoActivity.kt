package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.ImageButton
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MetodosPagoActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metodos_pago)

        barraNavegacion()
        
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        findViewById<android.widget.LinearLayout>(R.id.btnAgregarMetodo)?.setOnClickListener {
            showToast("Función para agregar método próximamente")
        }
        
        cargarMetodosPago()
    }

    private fun cargarMetodosPago() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Siguiendo tu especificación: usuarios -> {uid} -> metodos_pago
        db.collection("usuarios").document(uid).collection("metodos_pago")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    showToast("No hay métodos de pago registrados")
                }
            }
    }
}
