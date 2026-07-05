package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilTecnicoActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_tecnico)

        barraNavegacion()
        inicializarComponentes()
        configurarMenuLateral()
        cargarDatos()
    }

    private fun inicializarComponentes() {
        drawerLayout = findViewById(R.id.drawerLayout)
        
        findViewById<View>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.cardEditarPerfil)?.setOnClickListener {
            startActivity(Intent(this, EditarPerfilTecnicoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.cardMetodosPago)?.setOnClickListener {
            startActivity(Intent(this, MetodosPagoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.cardGanancias)?.setOnClickListener {
            startActivity(Intent(this, DashboardGananciasActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.cardZonasCobertura)?.setOnClickListener {
            startActivity(Intent(this, CoberturaLaboralActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnCerrarSesion)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun configurarMenuLateral() {
        val navView = findViewById<NavigationView>(R.id.navigationView)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_perfil -> {} // Ya estamos aquí
                R.id.nav_ganancias -> startActivity(Intent(this, DashboardGananciasActivity::class.java))
                R.id.nav_cobertura -> startActivity(Intent(this, CoberturaLaboralActivity::class.java))
                R.id.nav_solicitudes -> startActivity(Intent(this, MisSolicitudesActivity::class.java))
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                R.id.nav_soporte -> showToast("Soporte técnico próximamente")
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun cargarDatos() {
        if (uid == null) return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombreCompleto") ?: ""
                    val correo = document.getString("correo") ?: ""
                    val foto = document.getString("fotoPerfil") ?: ""

                    findViewById<TextView>(R.id.txtNombre).text = nombre
                    findViewById<TextView>(R.id.txtCorreo).text = correo

                    if (foto.isNotEmpty()) {
                        Glide.with(this).load(foto).circleCrop().into(findViewById<ImageView>(R.id.imgPerfil))
                    }
                    
                    // Actualizar Header del Drawer
                    val navView = findViewById<NavigationView>(R.id.navigationView)
                    val headerView = navView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.tvNavHeaderNombre).text = nombre
                    headerView.findViewById<TextView>(R.id.tvNavHeaderCorreo).text = correo
                    
                    val iniciales = obtenerIniciales(nombre)
                    headerView.findViewById<TextView>(R.id.tvNavHeaderInitials).text = iniciales
                    val imgNavHeader = headerView.findViewById<ImageView>(R.id.imgNavHeaderPerfil)
                    if (foto.isNotEmpty()) {
                        Glide.with(this).load(foto).circleCrop().into(imgNavHeader)
                        imgNavHeader.visibility = View.VISIBLE
                    }
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
