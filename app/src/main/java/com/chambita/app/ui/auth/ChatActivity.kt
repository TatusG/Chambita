package com.chambita.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ChatActivity : AppCompatActivity() {

    private val TAG = "CHAT_LOG"
    private var contactoId: String? = null
    private var chatId: String? = null
    private var contactoData: Usuario? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    
    private lateinit var adapter: ChatAdapter
    private var mensajesListener: ListenerRegistration? = null
    private var miRol: String = "cliente"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { subirImagenYEnviar(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactoId = intent.getStringExtra("contactoId") ?: intent.getStringExtra("tecnicoId")
        
        if (contactoId == null || currentUid == null) {
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarComponentes()
        obtenerRolYGenerarId()
        cargarDatosContacto()
    }

    private fun obtenerRolYGenerarId() {
        currentUid?.let { uid ->
            db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
                miRol = doc.getString("rol") ?: "cliente"
                
                chatId = if (miRol == "cliente") {
                    "${currentUid}_${contactoId}"
                } else {
                    "${contactoId}_${currentUid}"
                }
                
                Log.d(TAG, "Chat ID generado: $chatId | Mi Rol: $miRol")
                escucharMensajes()
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener rol: ${e.message}")
                chatId = "${currentUid}_${contactoId}"
                escucharMensajes()
            }
        }
    }

    private fun inicializarComponentes() {
        val rvMensajes = findViewById<RecyclerView>(R.id.rvMensajes)
        adapter = ChatAdapter(emptyList())
        rvMensajes.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMensajes.adapter = adapter

        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnEnviar).setOnClickListener {
            val et = findViewById<EditText>(R.id.etMensaje)
            val texto = et.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(texto, "texto")
                et.setText("")
            }
        }

        findViewById<ImageButton>(R.id.btnAdjuntar).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<ImageButton>(R.id.btnUbicacion).setOnClickListener {
            abrirMapaContacto()
        }
    }

    private fun cargarDatosContacto() {
        db.collection("usuarios").document(contactoId!!).get()
            .addOnSuccessListener { doc ->
                val contacto = doc.toObject(Usuario::class.java)
                contacto?.let {
                    contactoData = it
                    findViewById<TextView>(R.id.tvNombreTecnico).text = it.nombreCompleto
                    
                    val tvEstado = findViewById<TextView>(R.id.tvEstadoTecnico)
                    if (it.disponible) {
                        tvEstado.text = "En línea"
                        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.chambita_verde))
                    } else {
                        tvEstado.text = "Desconectado"
                        tvEstado.setTextColor(ContextCompat.getColor(this, R.color.chambita_rojo))
                    }
                    
                    val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
                    if (it.fotoPerfil.isNotEmpty()) {
                        Glide.with(this).load(it.fotoPerfil).circleCrop().into(imgPerfil)
                    }
                }
            }
    }

    private fun abrirMapaContacto() {
        val distrito = if (contactoData?.rol == "tecnico") {
            contactoData?.distritoActivoHoy
        } else {
            contactoData?.distritoResidencia
        }

        if (distrito.isNullOrEmpty()) {
            Toast.makeText(this, "El contacto no tiene una ubicación definida", Toast.LENGTH_SHORT).show()
            return
        }

        // Abrir Google Maps con la ubicación del contacto
        val gmmIntentUri = Uri.parse("geo:0,0?q=${distrito}, Lima, Peru")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback si no tiene app de mapas oficial
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${distrito},+Lima,+Peru"))
            startActivity(webIntent)
        }
    }

    private fun escucharMensajes() {
        if (chatId == null) return
        
        mensajesListener = db.collection("chats").document(chatId!!)
            .collection("mensajes")
            .orderBy("fechaRegistro", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error en listener: ${e.message}")
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Mensaje::class.java) ?: emptyList()
                adapter.updateList(lista)
                if (lista.isNotEmpty()) {
                    findViewById<RecyclerView>(R.id.rvMensajes).scrollToPosition(lista.size - 1)
                }
            }
    }

    private fun enviarMensaje(texto: String, tipo: String) {
        if (currentUid == null || chatId == null) return

        val nuevoMensaje = Mensaje(
            remitenteId = currentUid,
            texto = texto,
            tipo = tipo,
            leido = false,
            fechaRegistro = Timestamp.now()
        )

        val ids = chatId!!.split("_")
        val chatData = hashMapOf(
            "clienteId" to ids[0],
            "tecnicoId" to ids[1],
            "ultimoMensaje" to (if (tipo == "imagen") "📷 Imagen" else texto),
            "fechaUltimoMensaje" to Timestamp.now()
        )

        val batch = db.batch()
        val chatRef = db.collection("chats").document(chatId!!)
        val mensajeRef = chatRef.collection("mensajes").document()
        
        batch.set(chatRef, chatData, SetOptions.merge())
        batch.set(mensajeRef, nuevoMensaje)

        batch.commit().addOnFailureListener { e ->
            Log.e(TAG, "ERROR DE FIREBASE: ${e.message}")
            Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subirImagenYEnviar(uri: Uri) {
        if (chatId == null) return
        val path = "chats/${chatId}/${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(path)

        imageRef.putFile(uri).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                enviarMensaje(downloadUri.toString(), "imagen")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mensajesListener?.remove()
    }
}
