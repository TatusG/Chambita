package com.chambita.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.data.local.AppDatabase
import com.chambita.app.models.Mensaje
import com.chambita.app.models.Usuario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.*

class ChatActivity : NavActivity() {

    private val TAG = "CHAT_LOG"
    private var contactoId: String? = null
    private var chatId: String? = null
    private var contactoData: Usuario? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private var miRol: String = "cliente"
    
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMensajes: RecyclerView
    private var mensajesListener: ListenerRegistration? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { subirImagenYEnviar(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactoId = intent.getStringExtra("contactoId") ?: intent.getStringExtra("tecnicoId")
        
        if (contactoId.isNullOrEmpty() || currentUid == null) {
            Toast.makeText(this, "Error: Datos de chat incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarComponentes()
        determinarJerarquiaYActivarChat()
        cargarDatosContacto()
    }

    private fun determinarJerarquiaYActivarChat() {
        lifecycleScope.launch {
            val dbLocal = AppDatabase.getDatabase(this@ChatActivity)
            val session = dbLocal.userSessionDao().getActiveSession()
            miRol = session?.rol ?: "cliente"

            chatId = if (miRol == "cliente") "${currentUid}_${contactoId}" else "${contactoId}_${currentUid}"
            escucharMensajes()
        }
    }

    private fun inicializarComponentes() {
        rvMensajes = findViewById(R.id.rvMensajes)
        adapter = ChatAdapter(emptyList()) { mensaje, accion ->
            procesarAccionPropuesta(mensaje, accion)
        }
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
            mostrarOpcionesAdjuntar()
        }

        findViewById<ImageButton>(R.id.btnUbicacion).setOnClickListener {
            abrirMapaContacto()
        }
    }

    private fun mostrarOpcionesAdjuntar() {
        val opciones = if (miRol == "tecnico") arrayOf("Enviar Foto", "Enviar Propuesta de Precio") else arrayOf("Enviar Foto")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar acción")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> mostrarDialogoPropuesta()
                }
            }
            .show()
    }

    private fun mostrarDialogoPropuesta() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_registrar_pago, null)
        val etMonto = view.findViewById<EditText>(R.id.etMontoCobrado)
        view.findViewById<TextView>(R.id.tvLabelMetodoPago)?.visibility = View.GONE
        view.findViewById<RadioGroup>(R.id.rgMetodoPago)?.visibility = View.GONE

        AlertDialog.Builder(this)
            .setTitle("Enviar Propuesta de Precio")
            .setView(view)
            .setPositiveButton("Enviar") { _, _ ->
                val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
                if (monto > 0) enviarMensaje("Propuesta por el servicio", "propuesta", monto)
                else showToast("Ingresa un monto válido")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarAccionPropuesta(mensaje: Mensaje, accion: String) {
        if (chatId == null || mensaje.id.isEmpty()) return

        val nuevoEstado = if (accion == "ACEPTAR") "aceptada" else "rechazada"

        // ✅ Actualizar estado directamente usando el ID del mensaje
        db.collection("chats").document(chatId!!)
            .collection("mensajes").document(mensaje.id)
            .update("estadoPropuesta", nuevoEstado)
            .addOnSuccessListener {
                if (accion == "ACEPTAR") {
                    actualizarMontoSolicitud(mensaje.monto)
                    enviarMensaje("He aceptado la propuesta de S/ ${mensaje.monto.toInt()}", "texto")
                } else {
                    enviarMensaje("He rechazado la propuesta", "texto")
                }
            }
    }

    private fun actualizarMontoSolicitud(monto: Double) {
        // Buscamos la solicitud activa entre estos dos usuarios
        db.collection("solicitudes")
            .whereEqualTo("clienteId", if (miRol == "cliente") currentUid else contactoId)
            .whereEqualTo("tecnicoId", if (miRol == "tecnico") currentUid else contactoId)
            .whereIn("estado", listOf("pendiente", "aceptada", "en_curso"))
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val solId = snapshot.documents[0].id
                    db.collection("solicitudes").document(solId).update("montoFinal", monto)
                    showToast("Precio acordado: S/ $monto")
                }
            }
    }

    private fun enviarMensaje(texto: String, tipo: String, monto: Double = 0.0) {
        if (currentUid == null || chatId == null) return

        val nuevoMensaje = hashMapOf(
            "remitenteId"      to currentUid,
            "texto"            to texto,
            "tipo"             to tipo,
            "monto"            to monto,
            "leido"            to false,
            "estadoPropuesta"  to "pendiente",
            "fechaRegistro"    to com.google.firebase.Timestamp.now()
        )

        val ids = chatId!!.split("_")
        val chatData = hashMapOf(
            "clienteId"          to ids[0],
            "tecnicoId"          to ids[1],
            "ultimoMensaje"      to if (tipo == "propuesta") "💰 Propuesta: S/ ${monto.toInt()}" else texto,
            "fechaUltimoMensaje" to com.google.firebase.Timestamp.now()
        )

        val batch = db.batch()
        batch.set(
            db.collection("chats").document(chatId!!),
            chatData,
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.set(
            db.collection("chats").document(chatId!!).collection("mensajes").document(),
            nuevoMensaje
        )
        batch.commit()
            .addOnSuccessListener { Log.d("CHAT", "Mensaje enviado OK") }
            .addOnFailureListener { Log.e("CHAT", "Error al enviar: ${it.message}") }
    }

    private fun cargarDatosContacto() {
        db.collection("usuarios").document(contactoId!!).addSnapshotListener { doc, e ->
            if (e != null || doc == null) return@addSnapshotListener
            val contacto = doc.toObject(Usuario::class.java)
            contacto?.let {
                contactoData = it
                findViewById<TextView>(R.id.tvNombreTecnico).text = it.nombreCompleto
                val tvEstado = findViewById<TextView>(R.id.tvEstadoTecnico)
                
                if (it.estaEnLinea) {
                    tvEstado.text = "En línea"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.chambita_verde))
                } else {
                    tvEstado.text = "Desconectado"
                    tvEstado.setTextColor(ContextCompat.getColor(this, R.color.chambita_rojo))
                }
                
                if (it.fotoPerfil.isNotEmpty()) Glide.with(this).load(it.fotoPerfil).circleCrop().into(findViewById(R.id.imgPerfil))
            }
        }
    }

    private fun escucharMensajes() {
        if (chatId == null) return
        mensajesListener = db.collection("chats").document(chatId!!)
            .collection("mensajes")
            .orderBy("fechaRegistro", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Mensaje::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                adapter.updateList(lista)
                if (lista.isNotEmpty()) rvMensajes.scrollToPosition(lista.size - 1)
            }
    }

    private fun abrirMapaContacto() {
        val distrito = if (contactoData?.rol == "tecnico") contactoData?.distritoActivoHoy else contactoData?.distritoResidencia
        if (distrito.isNullOrEmpty()) return
        val gmmIntentUri = Uri.parse("geo:0,0?q=${distrito}, Lima, Peru")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try { startActivity(mapIntent) } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${distrito}")))
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
