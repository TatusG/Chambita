package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.LinearLayout
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MisDireccionesActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_direcciones)

        barraNavegacion()
        
        findViewById<LinearLayout>(R.id.btnVolver)?.setOnClickListener { finish() }
        
        cargarDirecciones()
    }

    private fun cargarDirecciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Siguiendo tu especificación: usuarios -> {uid} -> direcciones
        db.collection("usuarios").document(uid).collection("direcciones")
            .get()
            .addOnSuccessListener { result ->
                // Aquí conectarías con un Adapter para el RecyclerView
                if (result.isEmpty) {
                    showToast("No tienes direcciones guardadas")
                }
            }
    }
}
