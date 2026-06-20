package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MensajesActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vista_chat) // Layout de LISTA de chats

        barraNavegacion()
        
        // Configuraciones de la vista de lista
        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        val rvChats = findViewById<RecyclerView>(R.id.rvChats)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)

        // Cargar datos de la bandeja
        cargarBandejaEntrada()
        
        // En el futuro, al hacer clic en un item de rvChats, 
        // se abrirá ChatActivity pasando el ID del técnico.
    }

    private fun cargarBandejaEntrada() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("chats")
            .whereEqualTo("clienteId", uid)
            .orderBy("fechaUltimoMensaje", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    showToast("No tienes conversaciones activas")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error al cargar chats: ${e.message}")
            }
    }
}
