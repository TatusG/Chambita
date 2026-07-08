package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.data.repository.SolicitudRepository
import com.chambita.app.models.Usuario
import com.chambita.app.models.Solicitud
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.Locale

class HomeTecnicoActivity : NavActivity() {

    private val TAG = "HOME_TECNICO"
    private val db = FirebaseFirestore.getInstance()
    private val solicitudRepo = SolicitudRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    
    private lateinit var adapter: SolicitudTecnicoAdapter
    private lateinit var drawerLayout: DrawerLayout
    private var solicitudesListener: ListenerRegistration? = null
    private var misDistritos: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_tecnico)

        barraNavegacion()
        inicializarComponentes()
        configurarMenuLateral()
        cargarDatosPerfil()
    }

    private fun inicializarComponentes() {
        drawerLayout = findViewById(R.id.drawerLayout)
        
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        rv.layoutManager = LinearLayoutManager(this)
        
        adapter = SolicitudTecnicoAdapter(emptyList(), 
            onAceptar = { solicitud -> aceptarTrabajo(solicitud.id) },
            onRechazar = { _ -> showToast("Solicitud omitida") },
            onDetail = { solicitud -> mostrarDetalleSolicitud(solicitud) }
        )
        rv.adapter = adapter
        
        findViewById<TextView>(R.id.btnEditarZona)?.setOnClickListener {
            startActivity(Intent(this, CoberturaLaboralActivity::class.java))
        }

        findViewById<ImageView>(R.id.imgPerfil)?.setOnClickListener {
            startActivity(Intent(this, PerfilTecnicoActivity::class.java))
        }

        findViewById<SwitchCompat>(R.id.swDisponible).setOnCheckedChangeListener { _, isChecked ->
            actualizarDisponibilidad(isChecked)
        }
    }

    private fun mostrarDetalleSolicitud(solicitud: Solicitud) {
        AlertDialog.Builder(this)
            .setTitle("Detalle de la Avería")
            .setMessage("Cliente: ${solicitud.nombreCliente}\n\nDescripción:\n${solicitud.descripcionAveria}")
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Aceptar Trabajo") { _, _ -> aceptarTrabajo(solicitud.id) }
            .show()
    }

    private fun configurarMenuLateral() {
        val navView = findViewById<NavigationView>(R.id.navigationView)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_perfil -> startActivity(Intent(this, PerfilTecnicoActivity::class.java))
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

    private fun cargarDatosPerfil() {
        if (uid == null) return
        db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val tecnico = doc.toObject(Usuario::class.java)
                tecnico?.let {
                    findViewById<TextView>(R.id.txtNombreTecnico).text = it.nombreCompleto
                    
                    val cobertura = if (it.distritos.isNotEmpty()) {
                        it.distritos.joinToString(" · ")
                    } else {
                        "Sin zonas asignadas"
                    }
                    findViewById<TextView>(R.id.txtZona).text = cobertura
                    
                    findViewById<SwitchCompat>(R.id.swDisponible).isChecked = it.disponible
                    misDistritos = it.distritos
                    
                    actualizarEstadisticas(it)

                    // ACTUALIZAR DRAWER
                    val navView = findViewById<NavigationView>(R.id.navigationView)
                    if (navView != null && navView.headerCount > 0) {
                        val header = navView.getHeaderView(0)
                        header.findViewById<TextView>(R.id.tvNavHeaderNombre).text = it.nombreCompleto
                        header.findViewById<TextView>(R.id.tvNavHeaderCorreo).text = it.correo
                        val iniciales = obtenerIniciales(it.nombreCompleto)
                        header.findViewById<TextView>(R.id.tvNavHeaderInitials).text = iniciales
                        val imgNavHeader = header.findViewById<ImageView>(R.id.imgNavHeaderPerfil)
                        if (it.fotoPerfil.isNotEmpty()) {
                            Glide.with(this).load(it.fotoPerfil).circleCrop().into(imgNavHeader)
                            imgNavHeader.visibility = View.VISIBLE
                        }
                    }

                    if (it.fotoPerfil.isNotEmpty()) {
                        Glide.with(this).load(it.fotoPerfil).circleCrop().into(findViewById<ImageView>(R.id.imgPerfil))
                    }

                    if (it.disponible) activarEscuchaSolicitudes()
                }
            } else {
                Log.e(TAG, "Perfil no encontrado. Cerrando sesión.")
                forzarCerrarSesion()
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

    private fun forzarCerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        lifecycleScope.launch {
            val dbLocal = com.chambita.app.data.local.AppDatabase.getDatabase(this@HomeTecnicoActivity)
            dbLocal.userSessionDao().clearActiveSessions()
            val intent = Intent(this@HomeTecnicoActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun actualizarEstadisticas(t: Usuario) {
        val vMes    = findViewById<View>(R.id.statMes)
        val vGanado = findViewById<View>(R.id.statGanado)
        val vRating = findViewById<View>(R.id.statRating)

        // Labels
        vMes.findViewById<TextView>(R.id.txtTitulo).text    = "Trabajos"
        vGanado.findViewById<TextView>(R.id.txtTitulo).text = "Ganado"
        vRating.findViewById<TextView>(R.id.txtTitulo).text = "Rating"

        // Rating inmediato desde el objeto
        vRating.findViewById<TextView>(R.id.txtNumero).text =
            String.format(Locale.US, "%.1f ★", t.promedioEstrellas)

        // Trabajos completados
        db.collection("solicitudes")
            .whereEqualTo("tecnicoId", t.uid)
            .whereEqualTo("estado", "finalizada")
            .get()
            .addOnSuccessListener { snapshot ->
                vMes.findViewById<TextView>(R.id.txtNumero).text =
                    snapshot.size().toString()
            }
            .addOnFailureListener {
                vMes.findViewById<TextView>(R.id.txtNumero).text = "0"
            }

        // Ganancias totales
        db.collection("pagos")
            .whereEqualTo("tecnicoId", t.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                var total = 0.0
                snapshot.forEach { doc -> total += doc.getDouble("monto") ?: 0.0 }

                // ✅ Sin decimales si el monto es entero
                vGanado.findViewById<TextView>(R.id.txtNumero).text =
                    if (total % 1.0 == 0.0) {
                        "S/ ${total.toInt()}"
                    } else {
                        "S/ ${"%.0f".format(total)}"
                    }
            }
            .addOnFailureListener {
                vGanado.findViewById<TextView>(R.id.txtNumero).text = "S/ 0"
            }
    }

    private fun activarEscuchaSolicitudes() {
        if (solicitudesListener != null || uid == null) return
        
        if (misDistritos.isEmpty()) {
            Log.w(TAG, "El técnico no tiene distritos de cobertura configurados. No se mostrarán solicitudes abiertas.")
            return
        }

        Log.d(TAG, "Escuchando solicitudes para el técnico $uid en zonas: $misDistritos")
        solicitudesListener = solicitudRepo.escucharSolicitudesParaTecnico(uid, misDistritos) { lista ->
            Log.d(TAG, "Solicitudes detectadas para el técnico: ${lista.size}")
            adapter.updateList(lista)
            val tvPendientes = findViewById<TextView>(R.id.txtPendientes)
            tvPendientes.text = "${lista.size} pendiente"
            tvPendientes.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun aceptarTrabajo(solicitudId: String) {
        if (solicitudId.isEmpty()) {
            showToast("Error: ID de solicitud no válido")
            return
        }
        
        uid?.let { tecnicoId ->
            solicitudRepo.aceptarSolicitud(solicitudId, tecnicoId) {
                showToast("¡Trabajo aceptado!")
                startActivity(Intent(this, MisSolicitudesActivity::class.java))
            }
        }
    }

    private fun actualizarDisponibilidad(disponible: Boolean) {
        uid?.let {
            db.collection("usuarios").document(it).update("disponible", disponible)
                .addOnSuccessListener {
                    if (disponible) activarEscuchaSolicitudes() 
                    else desactivarEscuchaSolicitudes()
                }
        }
    }

    private fun desactivarEscuchaSolicitudes() {
        solicitudesListener?.remove()
        solicitudesListener = null
        adapter.updateList(emptyList())
    }

    override fun onDestroy() {
        super.onDestroy()
        desactivarEscuchaSolicitudes()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
