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
    private var clienteDistrito: String = ""
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
        configurarChips()
    }

    override fun onStart() {
        super.onStart()
        cargarDatosCabecera()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val filtros = intent.getStringArrayListExtra("filtroDistritos")
        if (filtros != null) {
            distritosPermitidos = filtros
            buscarTecnicosLocalized(null)
        }
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

        findViewById<TextView>(R.id.tvVerTodo)?.setOnClickListener { 
            showToast("Mostrando todos los técnicos de Lima")
            buscarTecnicosGeneral(null) 
        }

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

        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        findViewById<View>(R.id.layoutAvatar)?.setOnClickListener { startActivity(Intent(this, PerfilClienteActivity::class.java)) }
        findViewById<CardView>(R.id.cardMapa)?.setOnClickListener {
            if (clienteDistrito.isNotEmpty()) {
                startActivity(Intent(this, NuevaSolicitudActivity::class.java).apply { putExtra("distrito", clienteDistrito) })
            }
        }
    }

    private fun configurarMenuLateral() {
        val navView = findViewById<NavigationView>(R.id.navigationView)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_perfil -> startActivity(Intent(this, PerfilClienteActivity::class.java))
                R.id.nav_direcciones -> startActivity(Intent(this, MisDireccionesActivity::class.java))
                R.id.nav_logout -> forzarCerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun cargarDatosCabecera() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nombreCompleto = document.getString("nombreCompleto") ?: "Usuario"
                val primerNombre = nombreCompleto.trim().split(" ")[0]
                findViewById<TextView>(R.id.tvSaludo)?.text = "HOLA, ${primerNombre.uppercase()} 👋"

                clienteDistrito = document.getString("distritoResidencia") ?: ""
                if (clienteDistrito.isNotEmpty()) {
                    findViewById<TextView>(R.id.tvNombreZona)?.text = "Zona: $clienteDistrito"
                    cargarMapaEstatico(clienteDistrito)
                    cargarDistritosCercanos(clienteDistrito)
                } else {
                    findViewById<TextView>(R.id.tvNombreZona)?.text = "Zona: No definida"
                    buscarTecnicosGeneral(null)
                }
                
                val fotoPerfil = document.getString("fotoPerfil") ?: ""
                val correo = document.getString("correo") ?: ""
                actualizarUIPerfilHeader(nombreCompleto, correo, fotoPerfil)
            }
        }
    }

    private fun actualizarUIPerfilHeader(nombre: String, correo: String, foto: String) {
        val navView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tvNavHeaderNombre).text = nombre
        headerView.findViewById<TextView>(R.id.tvNavHeaderCorreo).text = correo
        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        val imgNavHeader = headerView.findViewById<ImageView>(R.id.imgNavHeaderPerfil)
        if (foto.isNotEmpty()) {
            Glide.with(this).load(foto).circleCrop().into(imgPerfil)
            Glide.with(this).load(foto).circleCrop().into(imgNavHeader)
            imgPerfil.visibility = View.VISIBLE
            imgNavHeader.visibility = View.VISIBLE
        }
    }

    private fun cargarDistritosCercanos(distrito: String) {
        db.collection("distritos").document(distrito).get()
            .addOnSuccessListener { doc ->
                val vecinos = doc.get("distritosVecinos") as? List<String> ?: emptyList()
                distritosPermitidos = (vecinos + distrito).distinct()
                buscarTecnicosLocalized(null)
            }
            .addOnFailureListener {
                distritosPermitidos = listOf(distrito)
                buscarTecnicosLocalized(null)
            }
    }

    private fun buscarTecnicosLocalized(especialidad: String?) {
        if (distritosPermitidos.isEmpty()) {
            Log.w(TAG, "No hay distritos permitidos configurados. Abortando búsqueda localizada.")
            return
        }
        
        Log.d(TAG, "Buscando técnicos localizados para especialidad: $especialidad")
        var query = db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)

        if (especialidad != null) query = query.whereEqualTo("especialidad", especialidad)

        query.get()
            .addOnSuccessListener { snapshot ->
                val listaCompleta = snapshot.mapNotNull { doc -> 
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id) 
                }
                
                // Filtrado tolerante a ortografía, guiones y variantes como Callao / Callao (Cercado)
                val listaFiltrada = listaCompleta.filter { tecnico ->
                    tecnico.distritos.any { td ->
                        distritosPermitidos.any { dp ->
                            matchesDistrict(td, dp)
                        }
                    }
                }
                Log.d(TAG, "Búsqueda localizada en memoria exitosa. Encontrados: ${listaFiltrada.size} de ${listaCompleta.size}")
                actualizarUILista(listaFiltrada)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error crítico en buscarTecnicosLocalized:", e)
                showToast("Error al cargar técnicos locales")
            }
    }

    private fun matchesDistrict(d1: String, d2: String): Boolean {
        val n1 = normalizarTexto(d1)
        val n2 = normalizarTexto(d2)
        if (n1 == n2) return true
        
        // Manejar "Callao" y "Callao (Cercado)"
        if (n1.contains("callao") && n2.contains("callao")) return true
        
        // Manejar "Cercado de Lima" y "Lima (Cercado)"
        if (n1.contains("lima") && n1.contains("cercado") && n2.contains("lima") && n2.contains("cercado")) return true
        
        return false
    }

    private fun normalizarTexto(texto: String?): String {
        if (texto == null) return ""
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("-", " ")
            .lowercase().trim()
    }

    private fun buscarTecnicosGeneral(especialidad: String?) {
        Log.d(TAG, "Buscando técnicos general (todos los distritos). Especialidad: $especialidad")
        var query = db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)
            
        if (especialidad != null) query = query.whereEqualTo("especialidad", especialidad)
        
        query.get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.mapNotNull { doc -> 
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id) 
                }
                Log.d(TAG, "Búsqueda general exitosa. Técnicos encontrados: ${lista.size}")
                actualizarUILista(lista)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error en buscarTecnicosGeneral:", e)
                showToast("Error al cargar la lista general")
            }
    }

    private fun buscarTecnicosPorTexto(texto: String) {
        db.collection("usuarios").whereEqualTo("rol", "tecnico").whereEqualTo("disponible", true).get()
            .addOnSuccessListener { snapshot ->
                val busqueda = texto.lowercase()
                val lista = snapshot.mapNotNull { it.toObject(Usuario::class.java)?.copy(uid = it.id) }
                    .filter { it.nombreCompleto.lowercase().contains(busqueda) || it.especialidad.lowercase().contains(busqueda) }
                actualizarUILista(lista)
            }
    }

    private fun actualizarUILista(lista: List<Usuario>) {
        tecnicoAdapter.actualizarLista(lista)
    }

    private fun configurarChips() {
        seleccionarChip(btnTodos)
        btnTodos.setOnClickListener { seleccionarChip(btnTodos); buscarTecnicosLocalized(null) }
        btnCategoria1.setOnClickListener { seleccionarChip(btnCategoria1); buscarTecnicosLocalized("Electricista") }
        btnCategoria2.setOnClickListener { seleccionarChip(btnCategoria2); buscarTecnicosLocalized("Gasfitero") }
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
                val center = "${distrito.replace(" ", "+")},+Lima,+Peru"
                val url = URL("https://maps.googleapis.com/maps/api/staticmap?center=$center&zoom=15&size=600x300&scale=2&maptype=roadmap&key=${com.chambita.app.BuildConfig.MAPS_API_KEY}")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("X-Android-Package", packageName)
                if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                    withContext(Dispatchers.Main) { imgMapa.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) { Log.e("MAPS", "Error", e) }
        }
    }

    private fun forzarCerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }
}
