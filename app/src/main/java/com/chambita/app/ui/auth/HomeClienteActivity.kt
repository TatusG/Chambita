package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeClienteActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cliente)

        // Activar navegación inferior
        barraNavegacion()
        cargarDatosCabecera()

        // Configurar botones de la cabecera
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            showToast("Menú lateral próximamente")
        }

        findViewById<ImageButton>(R.id.btnNotificaciones)?.setOnClickListener {
            showToast("No tienes notificaciones nuevas")
        }

        findViewById<ImageView>(R.id.imgPerfil)?.setOnClickListener {
            startActivity(Intent(this, PerfilClienteActivity::class.java))
        }

        // Configurar búsqueda y mapa
        findViewById<EditText>(R.id.etBuscar)?.setOnEditorActionListener { v, _, _ ->
            showToast("Buscando: ${v.text}")
            false
        }

        findViewById<CardView>(R.id.cardMapa)?.setOnClickListener {
            startActivity(Intent(this, NuevaSolicitudActivity::class.java))
        }
    }

    private fun cargarDatosCabecera() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombreCompleto = document.getString("nombreComplete") ?: "Usuario"
                    // Tomamos solo el primer nombre para el saludo
                    val primerNombre = nombreCompleto.split(" ")[0]
                    
                    findViewById<TextView>(R.id.tvSaludo)?.text = "HOLA, ${primerNombre.uppercase()}"
                }
            }
    }
}
