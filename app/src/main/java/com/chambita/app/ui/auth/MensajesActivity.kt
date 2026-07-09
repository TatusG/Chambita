package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.data.local.AppDatabase
import com.chambita.app.models.Chat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class MensajesActivity : NavActivity() {

    private lateinit var adapter: ChatListAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private var chatsListener: ListenerRegistration? = null
    private lateinit var drawerLayout: DrawerLayout
    private var allChats: List<Chat> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mensajes)

        barraNavegacion()
        inicializarComponentes()
        configurarMenuLateral()
        cargarMiAvatar()
        escucharChats()
    }

    private fun configurarMenuLateral() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navigationView)
        
        findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        lifecycleScope.launch {
            val dbLocal = AppDatabase.getDatabase(this@MensajesActivity)
            val session = dbLocal.userSessionDao().getActiveSession()
            val rol = session?.rol ?: "cliente"

            if (rol == "tecnico") {
                navView.menu.clear()
                navView.inflateMenu(R.menu.menu_drawer_tecnico)
            } else {
                navView.menu.clear()
                navView.inflateMenu(R.menu.menu_drawer_cliente)
            }

            navView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_perfil -> {
                        val target = if (rol == "tecnico") PerfilTecnicoActivity::class.java else PerfilClienteActivity::class.java
                        startActivity(Intent(this@MensajesActivity, target))
                    }
                    R.id.nav_direcciones -> startActivity(Intent(this@MensajesActivity, MisDireccionesActivity::class.java))
                    R.id.nav_pagos -> startActivity(Intent(this@MensajesActivity, MetodosPagoActivity::class.java))
                    R.id.nav_historial -> startActivity(Intent(this@MensajesActivity, HistorialPagosActivity::class.java))
                    R.id.nav_ganancias -> startActivity(Intent(this@MensajesActivity, DashboardGananciasActivity::class.java))
                    R.id.nav_cobertura -> startActivity(Intent(this@MensajesActivity, CoberturaLaboralActivity::class.java))
                    R.id.nav_solicitudes -> startActivity(Intent(this@MensajesActivity, MisSolicitudesActivity::class.java))
                    R.id.nav_logout -> {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this@MensajesActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        }
    }

    private fun cargarMiAvatar() {
        if (currentUid == null) return
        db.collection("usuarios").document(currentUid).get().addOnSuccessListener { doc ->
            val url = doc.getString("fotoPerfil")
            val nombre = doc.getString("nombreCompleto") ?: ""
            val correo = doc.getString("correo") ?: ""

            if (!url.isNullOrEmpty()) {
                val imgNavMini = findViewById<ImageView>(R.id.imgNavMini)
                if (imgNavMini != null) {
                    Glide.with(this).load(url).circleCrop().into(imgNavMini)
                }
            }

            // Actualizar Header del Drawer
            val navView = findViewById<NavigationView>(R.id.navigationView)
            if (navView.headerCount > 0) {
                val header = navView.getHeaderView(0)
                header.findViewById<TextView>(R.id.tvNavHeaderNombre)?.text = nombre
                header.findViewById<TextView>(R.id.tvNavHeaderCorreo)?.text = correo
                val imgHeader = header.findViewById<ImageView>(R.id.imgNavHeaderPerfil)
                if (!url.isNullOrEmpty() && imgHeader != null) {
                    Glide.with(this).load(url).circleCrop().into(imgHeader)
                }
            }
        }
    }

    private fun inicializarComponentes() {
        val rvChats = findViewById<RecyclerView>(R.id.rvBandejaMensajes)
        val etBuscar = findViewById<EditText>(R.id.etBuscarChat)

        adapter = ChatListAdapter(emptyList()) { chat, contacto ->
            val contactoId = if (currentUid == chat.clienteId) chat.tecnicoId else chat.clienteId

            if (contactoId.isNullOrEmpty()) {
                showToast("Error: No se pudo identificar el contacto")
                return@ChatListAdapter
            }

            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId",          chat.id)
                putExtra("contactoId",      contactoId)
                putExtra("nombreContacto",  contacto?.nombreCompleto ?: "")
            }
            startActivity(intent)
        }

        rvChats?.layoutManager = LinearLayoutManager(this)
        rvChats?.adapter = adapter

        etBuscar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarChats(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarChats(texto: String) {
        if (texto.isEmpty()) {
            adapter.updateList(allChats)
        } else {
            val filtrados = allChats.filter { it.ultimoMensaje.lowercase().contains(texto.lowercase()) }
            adapter.updateList(filtrados)
        }
    }

    private fun escucharChats() {
        if (currentUid == null) return

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

                    allChats = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(id = doc.id)
                    }?.sortedByDescending { it.fechaUltimoMensaje } ?: emptyList()
                    
                    adapter.updateList(allChats)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatsListener?.remove()
    }
}
