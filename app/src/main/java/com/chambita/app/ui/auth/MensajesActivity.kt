package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MensajesActivity : NavActivity() {

    private lateinit var adapter: ChatListAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private var chatsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vista_chat)

        barraNavegacion()
        inicializarComponentes()
        cargarMiAvatar()
        escucharChats()
    }

    private fun cargarMiAvatar() {
        if (currentUid == null) return
        db.collection("usuarios").document(currentUid).get().addOnSuccessListener { doc ->
            val url = doc.getString("fotoPerfil")
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).circleCrop().into(findViewById<ImageView>(R.id.imgAvatar))
            }
        }
    }

    private fun inicializarComponentes() {
        val rvChats = findViewById<RecyclerView>(R.id.rvChats)

        adapter = ChatListAdapter(emptyList()) { chat, contacto ->

            // ✅ Determinar el contactoId directamente desde el objeto Chat
            // sin depender de que contacto?.uid esté resuelto
            val contactoId = if (currentUid == chat.clienteId) {
                chat.tecnicoId
            } else {
                chat.clienteId
            }

            if (contactoId.isNullOrEmpty()) {
                showToast("Error: No se pudo identificar el contacto")
                return@ChatListAdapter
            }

            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId",          chat.id)
                putExtra("contactoId",      contactoId)           // ← desde el Chat directamente
                putExtra("nombreContacto",  contacto?.nombreCompleto ?: "")
            }
            startActivity(intent)
        }

        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = adapter

        findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            finish()
        }
    }

    private fun escucharChats() {
        if (currentUid == null) return

        // Escuchar como cliente O como tecnico
        val query = db.collection("chats")
            .whereArrayContainsAny("participantes", listOf(currentUid)) // Requiere campo 'participantes' array
            // O podemos usar dos listeners y mezclar, pero mejor ajustar el modelo de Firestore
            // Por ahora usaremos el plan original de GEMINI.md: clienteId o tecnicoId
            // Como no se puede hacer OR en Firestore facilmente sin participantes array,
            // usaremos la busqueda por clienteId para esta demo si es cliente.
        
        // Mejoramos la consulta segun el rol
        db.collection("usuarios").document(currentUid).get().addOnSuccessListener { userDoc ->
            val rol = userDoc.getString("rol") ?: "cliente"
            val field = if (rol == "tecnico") "tecnicoId" else "clienteId"
            
            chatsListener = db.collection("chats")
                .whereEqualTo(field, currentUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("MensajesActivity", "Error escuchando chats", e)
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.mapNotNull { doc ->
                        doc.toObject(Chat::class.java).copy(id = doc.id)
                    }?.sortedByDescending { it.fechaUltimoMensaje } ?: emptyList()
                    
                    adapter.updateList(lista)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatsListener?.remove()
    }
}
