package com.chambita.app.ui.auth

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Mensaje
import com.chambita.app.models.Usuario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"
    private var contactoId: String? = null
    private var chatId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    
    private lateinit var adapter: ChatAdapter
    private var mensajesListener: ListenerRegistration? = null
    private var miRol: String = "cliente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactoId = intent.getStringExtra("contactoId") ?: intent.getStringExtra("tecnicoId")
        
        if (contactoId == null) {
            Toast.makeText(this, "Error: Contacto no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatId = intent.getStringExtra("chatId") ?: generarChatId()

        inicializarComponentes()
        obtenerMiRol()
        cargarDatosContacto()
        escucharMensajes()
    }

    private fun generarChatId(): String {
        val ids = listOf(currentUid ?: "", contactoId ?: "").sorted()
        return "${ids[0]}_${ids[1]}"
    }

    private fun obtenerMiRol() {
        currentUid?.let { uid ->
            db.collection("usuarios").document(uid).get().addOnSuccessListener {
                miRol = it.getString("rol") ?: "cliente"
            }
        }
    }

    private fun inicializarComponentes() {
        val rvMensajes = findViewById<RecyclerView>(R.id.rvMensajes)
        adapter = ChatAdapter(emptyList())
        rvMensajes.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMensajes.adapter = adapter

        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnEnviar).setOnClickListener {
            val et = findViewById<EditText>(R.id.etMensaje)
            val texto = et.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(texto)
                et.setText("")
            }
        }
    }

    private fun cargarDatosContacto() {
        db.collection("usuarios").document(contactoId!!).get()
            .addOnSuccessListener { doc ->
                val contacto = doc.toObject(Usuario::class.java)
                contacto?.let {
                    findViewById<TextView>(R.id.tvNombreTecnico).text = it.nombreCompleto
                    findViewById<TextView>(R.id.tvEstadoTecnico).text = if (it.disponible) "En línea" else "Desconectado"
                    
                    val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
                    if (it.fotoPerfil.isNotEmpty()) {
                        Glide.with(this).load(it.fotoPerfil).circleCrop().into(imgPerfil)
                    }
                }
            }
    }

    private fun escucharMensajes() {
        mensajesListener = db.collection("chats").document(chatId!!)
            .collection("mensajes")
            .orderBy("fechaRegistro", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error escuchando mensajes", e)
                    return@addSnapshotListener
                }

                val lista = snapshot?.toObjects(Mensaje::class.java) ?: emptyList()
                Log.d(TAG, "Mensajes recibidos: ${lista.size}")
                adapter.updateList(lista)
                if (lista.isNotEmpty()) {
                    findViewById<RecyclerView>(R.id.rvMensajes).smoothScrollToPosition(lista.size - 1)
                }
            }
    }

    private fun enviarMensaje(texto: String) {
        if (currentUid == null || chatId == null) return

        val nuevoMensaje = Mensaje(
            remitenteId = currentUid,
            texto = texto,
            leido = false,
            fechaRegistro = Timestamp.now()
        )

        val batch = db.batch()
        
        // 1. Agregar mensaje a la subcolección
        val mensajeRef = db.collection("chats").document(chatId!!).collection("mensajes").document()
        batch.set(mensajeRef, nuevoMensaje)

        // 2. Crear/Actualizar el documento del chat (Usamos merge para que no falle si no existe)
        val chatData = mutableMapOf<String, Any>(
            "ultimoMensaje" to texto,
            "fechaUltimoMensaje" to Timestamp.now()
        )
        
        // Aseguramos que los IDs de participantes estén presentes
        if (miRol == "cliente") {
            chatData["clienteId"] = currentUid
            chatData["tecnicoId"] = contactoId!!
        } else {
            chatData["clienteId"] = contactoId!!
            chatData["tecnicoId"] = currentUid
        }

        val chatRef = db.collection("chats").document(chatId!!)
        batch.set(chatRef, chatData, SetOptions.merge())

        batch.commit().addOnFailureListener { e ->
            Log.e(TAG, "Error al enviar mensaje", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mensajesListener?.remove()
    }
    
    private fun showToast(m: String) {
        android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show()
    }
}
