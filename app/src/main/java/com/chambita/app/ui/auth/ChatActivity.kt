package com.chambita.app.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"
    private var contactoId: String? = null
    private var chatId: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    
    private lateinit var adapter: ChatAdapter
    private var mensajesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactoId = intent.getStringExtra("contactoId")
        chatId = intent.getStringExtra("chatId") ?: generarChatId()

        inicializarComponentes()
        cargarDatosContacto()
        escucharMensajes()
    }

    private fun generarChatId(): String {
        val ids = listOf(currentUid ?: "", contactoId ?: "").sorted()
        return "${ids[0]}_${ids[1]}"
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
            val texto = findViewById<EditText>(R.id.etMensaje).text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(texto)
                findViewById<EditText>(R.id.etMensaje).setText("")
            }
        }
    }

    private fun cargarDatosContacto() {
        if (contactoId == null) return
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
        if (chatId == null) return
        
        mensajesListener = db.collection("chats").document(chatId!!)
            .collection("mensajes")
            .orderBy("fechaRegistro", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error escuchando mensajes", e)
                    return@addSnapshotListener
                }

                val lista = snapshot?.toObjects(Mensaje::class.java) ?: emptyList()
                adapter.updateList(lista)
                if (lista.isNotEmpty()) {
                    findViewById<RecyclerView>(R.id.rvMensajes).smoothScrollToPosition(lista.size - 1)
                }
            }
    }

    private fun enviarMensaje(texto: String) {
        if (currentUid == null || chatId == null) return

        val nuevoMensaje = Mensaje(
            id = "", // Firestore generará el ID
            remitenteId = currentUid,
            texto = texto,
            leido = false,
            fechaRegistro = Timestamp.now()
        )

        val batch = db.batch()
        
        // 1. Agregar mensaje a la subcolección
        val mensajeRef = db.collection("chats").document(chatId!!).collection("mensajes").document()
        batch.set(mensajeRef, nuevoMensaje)

        // 2. Actualizar el documento del chat para la bandeja de entrada
        val chatRef = db.collection("chats").document(chatId!!)
        batch.update(chatRef, mapOf(
            "ultimoMensaje" to texto,
            "fechaUltimoMensaje" to Timestamp.now(),
            "clienteId" to (if (chatId!!.startsWith(currentUid)) currentUid else contactoId),
            "tecnicoId" to (if (chatId!!.endsWith(currentUid)) currentUid else contactoId)
        ))

        batch.commit().addOnFailureListener { e ->
            Log.e(TAG, "Error al enviar mensaje", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mensajesListener?.remove()
    }
}
