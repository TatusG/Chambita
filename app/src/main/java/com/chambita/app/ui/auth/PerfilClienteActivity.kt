package com.chambita.app.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class PerfilClienteActivity : NavActivity() {

    private val LOCATION_PERMISSION_REQ_CODE = 1000
    private val viewModel: PerfilViewModel by viewModels { AuthViewModelFactory(this) }
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_cliente)

        barraNavegacion()
        inicializarComponentes()
        configurarMenuLateral()
        cargarDatosUsuario()
        observarEstado()
    }

    private fun inicializarComponentes() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val swNotificaciones = findViewById<Switch>(R.id.swNotificaciones)
        val swGps = findViewById<Switch>(R.id.swGps)

        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        swNotificaciones?.setOnCheckedChangeListener { _, isChecked ->
            actualizarPreferencia("notificacionesHabilitadas", isChecked)
        }

        swGps?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                verificarPermisoUbicacion()
            }
        }

        findViewById<LinearLayout>(R.id.opEditarPerfil)?.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val rol = doc.getString("rol")
                    if (rol == "tecnico") {
                        startActivity(Intent(this, EditarPerfilTecnicoActivity::class.java))
                    } else {
                        startActivity(Intent(this, EditarPerfilActivity::class.java))
                    }
                }
        }

        findViewById<LinearLayout>(R.id.opMetodosPago)?.setOnClickListener {
            startActivity(Intent(this, MetodosPagoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.opDirecciones)?.setOnClickListener {
            startActivity(Intent(this, MisDireccionesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.opHistorialPagos)?.setOnClickListener {
            startActivity(Intent(this, HistorialPagosActivity::class.java))
        }

        findViewById<Button>(R.id.btnCerrarSesion)?.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun configurarMenuLateral() {
        val navView = findViewById<NavigationView>(R.id.navigationView)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_perfil -> {} // Ya estamos aquí
                R.id.nav_direcciones -> startActivity(Intent(this, MisDireccionesActivity::class.java))
                R.id.nav_pagos -> startActivity(Intent(this, MetodosPagoActivity::class.java))
                R.id.nav_historial -> startActivity(Intent(this, HistorialPagosActivity::class.java))
                R.id.nav_logout -> {
                    viewModel.logout()
                }
                R.id.nav_soporte -> showToast("Soporte técnico próximamente")
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            viewModel.logoutEvent.collect {
                val intent = Intent(this@PerfilClienteActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun cargarDatosUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombreCompleto = document.getString("nombreCompleto") ?: "Usuario"
                    val correo = document.getString("correo") ?: ""
                    val fotoUrl = document.getString("fotoPerfil")
                    
                    findViewById<TextView>(R.id.txtNombre)?.text = nombreCompleto
                    findViewById<TextView>(R.id.txtCorreoRol)?.text = "$correo • Cliente"
                    
                    val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
                    if (!fotoUrl.isNullOrEmpty()) {
                        imgPerfil?.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(fotoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .into(imgPerfil!!)
                    } else {
                        imgPerfil?.visibility = View.GONE
                    }

                    val iniciales = obtenerIniciales(nombreCompleto)
                    findViewById<TextView>(R.id.tvAvatarInitials)?.text = iniciales

                    // Actualizar Header del Drawer
                    val navView = findViewById<NavigationView>(R.id.navigationView)
                    val headerView = navView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.tvNavHeaderNombre).text = nombreCompleto
                    headerView.findViewById<TextView>(R.id.tvNavHeaderCorreo).text = correo
                    headerView.findViewById<TextView>(R.id.tvNavHeaderInitials).text = iniciales
                    val imgNavHeader = headerView.findViewById<ImageView>(R.id.imgNavHeaderPerfil)
                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(fotoUrl).circleCrop().into(imgNavHeader)
                        imgNavHeader.visibility = View.VISIBLE
                    }

                    val habilitadas = document.getBoolean("notificacionesHabilitadas") ?: false
                    findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swNotificaciones)?.isChecked = habilitadas
                }
            }
    }

    private fun obtenerIniciales(nombre: String): String {
        val partes = nombre.trim().split("\\s+".toRegex())
        return if (partes.size >= 2) {
            "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        } else {
            nombre.take(2).uppercase()
        }
    }

    private fun actualizarPreferencia(campo: String, valor: Any) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .update(campo, valor)
            .addOnFailureListener { e -> showToast("Error al guardar preferencia: ${e.message}") }
    }

    private fun verificarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE)
        } else {
            showToast("Permiso de ubicación ya concedido")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQ_CODE) {
            val swGps = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swGps)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Ubicación activada correctamente")
            } else {
                showToast("Permiso denegado. No se puede activar el GPS.")
                swGps?.isChecked = false
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
