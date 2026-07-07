package com.chambita.app.ui.auth

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Usuario
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

class HomeClienteActivity : NavActivity() {

    private val TAG = "HOME_CLIENTE"
    private lateinit var tecnicoAdapter: TecnicoAdapter
    private val db = FirebaseFirestore.getInstance()
    private var clienteDistrito: String = "Ventanilla"
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var btnTodos: Button
    private lateinit var btnCategoria1: Button
    private lateinit var btnCategoria2: Button
    
    private var distritosPermitidos: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cliente)

        barraNavegacion()
        inicializarComponentes()
        configurarMenuLateral()
        cargarDatosCabecera()
        configurarChips()
    }

    private fun inicializarComponentes() {
        drawerLayout = findViewById(R.id.drawerLayout)
        
        btnTodos      = findViewById(R.id.btnTodos)
        btnCategoria1 = findViewById(R.id.btnCategoria1)
        btnCategoria2 = findViewById(R.id.btnCategoria2)

        val rvTecnicos = findViewById<RecyclerView>(R.id.rvTecnicos)
        tecnicoAdapter = TecnicoAdapter(emptyList()) { tecnico ->
            val intent = Intent(this, VistaClientePerfilTecnicoActivity::class.java).apply {
                putExtra("tecnicoUid", tecnico.uid)
            }
            startActivity(intent)
        }
        rvTecnicos?.layoutManager = LinearLayoutManager(this)
        rvTecnicos?.adapter = tecnicoAdapter

        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        etBuscar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString().trim()
                if (texto.isNotEmpty()) buscarTecnicosPorTexto(texto)
                else buscarTecnicosLocalized(null)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<ImageButton>(R.id.btnNotificaciones)?.setOnClickListener {
            showToast("No tienes notificaciones nuevas")
        }

        findViewById<View>(R.id.layoutAvatar)?.setOnClickListener {
            startActivity(Intent(this, PerfilClienteActivity::class.java))
        }

        findViewById<CardView>(R.id.cardMapa)?.setOnClickListener {
            startActivity(Intent(this, NuevaSolicitudActivity::class.java).apply {
                putExtra("distrito", clienteDistrito)
            })
        }
        
        findViewById<View>(R.id.fabAdd)?.setOnClickListener {
            startActivity(Intent(this, NuevaSolicitudActivity::class.java).apply {
                putExtra("distrito", clienteDistrito)
            })
        }
    }

    private fun configurarMenuLateral() {
        val navView = findViewById<NavigationView>(R.id.navigationView)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_perfil -> startActivity(Intent(this, PerfilClienteActivity::class.java))
                R.id.nav_direcciones -> startActivity(Intent(this, MisDireccionesActivity::class.java))
                R.id.nav_pagos -> startActivity(Intent(this, MetodosPagoActivity::class.java))
                R.id.nav_historial -> startActivity(Intent(this, HistorialPagosActivity::class.java))
                R.id.nav_logout -> forzarCerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun cargarDatosCabecera() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombreCompleto = document.getString("nombreCompleto") ?: "Usuario"
                    val primerNombre = nombreCompleto.trim().split(" ")[0]
                    val fotoPerfil = document.getString("fotoPerfil") ?: ""
                    val correo = document.getString("correo") ?: ""

                    findViewById<TextView>(R.id.tvSaludo)?.text = "HOLA, ${primerNombre.uppercase()} 👋"

                    val iniciales = if (nombreCompleto.contains(" ")) {
                        val partes = nombreCompleto.trim().split(" ")
                        "${partes[0].take(1)}${partes[1].take(1)}"
                    } else {
                        nombreCompleto.take(2)
                    }.uppercase()

                    // Actualizar Home
                    findViewById<TextView>(R.id.tvAvatarInitials)?.text = iniciales
                    val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)

                    // Actualizar Drawer
                    val navView = findViewById<NavigationView>(R.id.navigationView)
                    val headerView = navView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.tvNavHeaderNombre).text = nombreCompleto
                    headerView.findViewById<TextView>(R.id.tvNavHeaderCorreo).text = correo
                    headerView.findViewById<TextView>(R.id.tvNavHeaderInitials).text = iniciales
                    val imgNavHeader = headerView.findViewById<ImageView>(R.id.imgNavHeaderPerfil)

                    if (fotoPerfil.isNotEmpty()) {
                        Glide.with(this).load(fotoPerfil).circleCrop().into(imgPerfil)
                        imgPerfil.visibility = View.VISIBLE

                        Glide.with(this).load(fotoPerfil).circleCrop().into(imgNavHeader)
                        imgNavHeader.visibility = View.VISIBLE
                    } else {
                        imgPerfil.visibility = View.GONE
                        imgNavHeader.visibility = View.GONE
                    }

                    clienteDistrito = document.getString("distritoResidencia") ?: "Ventanilla"
                    findViewById<TextView>(R.id.tvNombreZona)?.text = "Zona: $clienteDistrito"
                    
                    cargarMapaEstatico(clienteDistrito)
                    
                    // PASO 1: Obtener vecinos antes de buscar técnicos
                    cargarDistritosCercanos(clienteDistrito)
                } else {
                    forzarCerrarSesion()
                }
            }
    }

    private fun cargarDistritosCercanos(distrito: String) {
        db.collection("distritos").document(distrito).get()
            .addOnSuccessListener { doc ->
                val vecinos = doc.get("distritosVecinos") as? List<String> ?: emptyList()
                // Lista final: Mi distrito + Vecinos
                distritosPermitidos = (vecinos + distrito).distinct()
                Log.d(TAG, "Zonas permitidas para búsqueda: $distritosPermitidos")
                
                // PASO 2: Ahora sí buscamos técnicos solo en estas zonas
                buscarTecnicosLocalized(null)
            }
            .addOnFailureListener {
                // Fallback: si falla la colección de distritos, buscamos solo en el distrito del cliente
                distritosPermitidos = listOf(distrito)
                buscarTecnicosLocalized(null)
            }
    }

    private fun buscarTecnicosLocalized(especialidad: String?) {
        if (distritosPermitidos.isEmpty()) return

        // Filtro: Rol Técnico + Disponible + Que cubra alguna de mis zonas permitidas
        var query = db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)
            .whereArrayContainsAny("distritos", distritosPermitidos)

        if (especialidad != null) {
            query = query.whereEqualTo("especialidad", especialidad)
        }

        query.get()
            .addOnSuccessListener { querySnapshot ->
                val lista = querySnapshot.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id)
                }
                actualizarUILista(lista)
            }
    }

    private fun buscarTecnicosPorTexto(texto: String) {
        // En la búsqueda manual por texto, sí permitimos buscar en todo Lima
        db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listaCompleta = querySnapshot.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id)
                }
                
                val busquedaLower = texto.lowercase(Locale.getDefault())
                val listaFiltrada = listaCompleta.filter { tecnico ->
                    tecnico.nombreCompleto.lowercase(Locale.getDefault()).contains(busquedaLower) ||
                    tecnico.especialidad.lowercase(Locale.getDefault()).contains(busquedaLower) ||
                    tecnico.distritos.any { d -> d.lowercase(Locale.getDefault()).contains(busquedaLower) }
                }
                actualizarUILista(listaFiltrada)
            }
    }

    private fun actualizarUILista(lista: List<Usuario>) {
        tecnicoAdapter.actualizarLista(lista)
        if (lista.isEmpty() && distritosPermitidos.isNotEmpty()) {
            val intent = Intent(this, SinResultadosActivity::class.java).apply {
                putExtra("distrito", clienteDistrito)
            }
            startActivity(intent)
        }
    }

    private fun configurarChips() {
        seleccionarChip(btnTodos)
        btnTodos.setOnClickListener {
            seleccionarChip(btnTodos)
            buscarTecnicosLocalized(null)
        }
        btnCategoria1.setOnClickListener {
            seleccionarChip(btnCategoria1)
            buscarTecnicosLocalized("Electricista")
        }
        btnCategoria2.setOnClickListener {
            seleccionarChip(btnCategoria2)
            buscarTecnicosLocalized("Gasfitero")
        }
    }

    private fun seleccionarChip(seleccionado: Button) {
        listOf(btnTodos, btnCategoria1, btnCategoria2).forEach { chip ->
            if (chip == seleccionado) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(getColor(R.color.blanco))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(getColor(R.color.chambita_texto_principal))
            }
        }
    }

    private fun cargarMapaEstatico(distrito: String) {
        val imgMapa = findViewById<ImageView>(R.id.imgMapa) ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = com.chambita.app.BuildConfig.MAPS_API_KEY
                val center = "${distrito.replace(" ", "+")},+Lima,+Peru"
                val urlString = "https://maps.googleapis.com/maps/api/staticmap?center=$center&zoom=15&size=600x300&scale=2&maptype=roadmap&key=$apiKey"
                val url = URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("X-Android-Package", packageName)
                if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                    withContext(Dispatchers.Main) { imgMapa.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) { Log.e("MAPS", "Error en mapa", e) }
        }
    }

    private fun forzarCerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }
}
