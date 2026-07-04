package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.data.repository.SolicitudRepository
import com.chambita.app.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.Locale

class HomeTecnicoActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val solicitudRepo = SolicitudRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    
    private lateinit var adapter: SolicitudTecnicoAdapter
    private var solicitudesListener: ListenerRegistration? = null
    private var misDistritos: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_tecnico)

        barraNavegacion()
        inicializarComponentes()
        cargarDatosPerfil()
    }

    private fun inicializarComponentes() {
        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        rv.layoutManager = LinearLayoutManager(this)
        
        adapter = SolicitudTecnicoAdapter(emptyList(), 
            onAceptar = { solicitud -> aceptarTrabajo(solicitud.id) },
            onRechazar = { solicitud -> showToast("Solicitud omitida") }
        )
        rv.adapter = adapter
        
        findViewById<TextView>(R.id.btnEditarZona)?.setOnClickListener {
            startActivity(Intent(this, EditarPerfilTecnicoActivity::class.java))
        }

        findViewById<ImageView>(R.id.imgPerfil)?.setOnClickListener {
            startActivity(Intent(this, PerfilTecnicoActivity::class.java))
        }

        findViewById<SwitchCompat>(R.id.swDisponible).setOnCheckedChangeListener { _, isChecked ->
            actualizarDisponibilidad(isChecked)
        }
    }

    private fun cargarDatosPerfil() {
        if (uid == null) return
        db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val tecnico = doc.toObject(Usuario::class.java)
                tecnico?.let {
                    findViewById<TextView>(R.id.txtNombreTecnico).text = it.nombreCompleto
                    findViewById<TextView>(R.id.txtZona).text = it.distritoActivoHoy
                    findViewById<SwitchCompat>(R.id.swDisponible).isChecked = it.disponible
                    misDistritos = it.distritos
                    
                    actualizarEstadisticas(it)

                    if (it.fotoPerfil.isNotEmpty()) {
                        Glide.with(this).load(it.fotoPerfil).circleCrop().into(findViewById<ImageView>(R.id.imgPerfil))
                    }

                    if (it.disponible) activarEscuchaSolicitudes()
                }
            } else {
                Log.e("HOME_TECNICO", "Perfil no encontrado. Cerrando sesión.")
                forzarCerrarSesion()
            }
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
        val vMes = findViewById<View>(R.id.statMes)
        vMes.findViewById<TextView>(R.id.txtNumero).text = t.conteoTrabajos.toString()
        vMes.findViewById<TextView>(R.id.txtTitulo).text = "Este mes"

        val vGanado = findViewById<View>(R.id.statGanado)
        vGanado.findViewById<TextView>(R.id.txtNumero).text = "S/ 0"
        vGanado.findViewById<TextView>(R.id.txtTitulo).text = "Ganado"

        val vRating = findViewById<View>(R.id.statRating)
        vRating.findViewById<TextView>(R.id.txtNumero).text = String.format(Locale.getDefault(), "%.1f ★", t.promedioEstrellas)
        vRating.findViewById<TextView>(R.id.txtTitulo).text = "Rating"
    }

    private fun activarEscuchaSolicitudes() {
        if (solicitudesListener != null || misDistritos.isEmpty()) return
        
        solicitudesListener = solicitudRepo.escucharSolicitudesNuevas(misDistritos) { lista ->
            adapter.updateList(lista)
            findViewById<TextView>(R.id.txtPendientes).text = "${lista.size} pendiente"
            findViewById<TextView>(R.id.txtPendientes).visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun aceptarTrabajo(solicitudId: String) {
        uid?.let { tecnicoId ->
            solicitudRepo.aceptarSolicitud(solicitudId, tecnicoId) {
                showToast("¡Trabajo aceptado! Ve a 'Mis Solicitudes' para ver detalles.")
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
}
