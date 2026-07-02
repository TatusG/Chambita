package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
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
        escucharChats()
    }

    private fun inicializarComponentes() {
        val rvChats = findViewById<RecyclerView>(R.id.rvChats)
        adapter = ChatListAdapter(emptyList()) { chat, contacto ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId", chat.id)
                putExtra("contactoId", contacto?.uid)
                putExtra("nombreContacto", contacto?.nombreCompleto)
            }
            startActivity(intent)
        }
        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = adapter
        
        findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            // Manejar apertura de drawer si fuera necesario o finish si es solo volver
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
                .orderBy("fechaUltimoMensaje", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("MensajesActivity", "Error escuchando chats", e)
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.mapNotNull { doc ->
                        doc.toObject(Chat::class.java).copy(id = doc.id)
                    } ?: emptyList()
                    
                    adapter.updateList(lista)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatsListener?.remove()
    }
}
