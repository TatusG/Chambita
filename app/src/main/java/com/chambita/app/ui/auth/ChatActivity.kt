package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private var tecnicoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat) // Layout de conversacion 1:1

        tecnicoId = intent.getStringExtra("tecnicoId")
        val nombreTecnico = intent.getStringExtra("nombreTecnico")

        val btnVolver = findViewById<ImageView>(R.id.btnVolver)
        val tvNombreTecnico = findViewById<TextView>(R.id.tvNombreTecnico)
        val etMensaje = findViewById<EditText>(R.id.etMensaje)
        val btnEnviar = findViewById<ImageButton>(R.id.btnEnviar)

        // Mostrar nombre si se pasó por intent, sino buscarlo
        if (nombreTecnico != null) {
            tvNombreTecnico.text = nombreTecnico
        } else if (tecnicoId != null) {
            cargarDatosTecnico(tecnicoId!!)
        }

        // BOTÓN VOLVER (Soluciona tu problema de no poder salir)
        btnVolver?.setOnClickListener {
            finish()
        }

        btnEnviar?.setOnClickListener {
            val mensaje = etMensaje.text.toString()
            if (mensaje.isNotEmpty()) {
                enviarMensaje(mensaje)
                etMensaje.setText("")
            }
        }
    }

    private fun cargarDatosTecnico(id: String) {
        FirebaseFirestore.getInstance().collection("usuarios").document(id).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombreCompleto")
                    findViewById<TextView>(R.id.tvNombreTecnico).text = nombre
                }
            }
    }

    private fun enviarMensaje(texto: String) {
        // Lógica para guardar en la subcolección 'mensajes'
        // Siguiendo tu estructura: chats -> {id_chat} -> mensajes
    }
}
