package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.data.local.AppDatabase
import com.chambita.app.data.repository.SolicitudRepository
import com.chambita.app.models.Solicitud
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class MisSolicitudesActivity : NavActivity() {

    private lateinit var adapterTecnico: SolicitudGestionTecnicoAdapter
    private lateinit var adapterCliente: SolicitudAdapter
    private val solicitudRepo = SolicitudRepository()
    private var solicitudesListener: ListenerRegistration? = null
    
    private var userRol: String = "cliente"
    private var misDistritos: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_solicitudes)

        barraNavegacion()
        inicializarComponentes()
        determinarRolYProceder()
    }

    private fun inicializarComponentes() {
        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        rv.layoutManager = LinearLayoutManager(this)

        adapterTecnico = SolicitudGestionTecnicoAdapter(emptyList(),
            onAceptar = { solicitud -> aceptarTrabajo(solicitud.id) },
            onRechazar = { showToast("Omitido") },
            onChat = { solicitud -> abrirChat(solicitud.clienteId) },
            onFinalizar = { solicitud -> finalizarTrabajo(solicitud.id) }
        )
        
        adapterCliente = SolicitudAdapter(emptyList()) { solicitud, accion ->
            if (accion == "CHAT") {
                abrirChat(solicitud.tecnicoId ?: "")
            } else if (accion == "CALIFICAR") {
                val intent = Intent(this, CalificarServicioActivity::class.java).apply {
                    putExtra("solicitudId", solicitud.id)
                    putExtra("tecnicoId", solicitud.tecnicoId)
                }
                startActivity(intent)
            }
        }

        setupFiltros()
    }

    private fun abrirChat(contactoId: String) {
        if (contactoId.isEmpty()) return
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("contactoId", contactoId)
        }
        startActivity(intent)
    }

    private fun determinarRolYProceder() {
        lifecycleScope.launch {
            val dbLocal = AppDatabase.getDatabase(this@MisSolicitudesActivity)
            val session = dbLocal.userSessionDao().getActiveSession()
            
            userRol = session?.rol ?: "cliente"
            
            if (userRol == "tecnico") {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        misDistritos = doc.get("distritos") as? List<String> ?: emptyList()
                        findViewById<RecyclerView>(R.id.rvSolicitudes).adapter = adapterTecnico
                        cargarSolicitudesTecnico("PENDIENTES")
                    }
            } else {
                findViewById<RecyclerView>(R.id.rvSolicitudes).adapter = adapterCliente
                findViewById<Button>(R.id.btnFinalizados)?.text = "HISTORIAL"
                cargarSolicitudesCliente("ACTIVOS")
            }
        }
    }

    private fun setupFiltros() {
        val btnPendientes = findViewById<Button>(R.id.btnPendientes)
        val btnEnCurso = findViewById<Button>(R.id.btnEnCurso)
        val btnFinalizados = findViewById<Button>(R.id.btnFinalizados)
        val botones = listOf(btnPendientes, btnEnCurso, btnFinalizados)

        btnPendientes?.setOnClickListener { 
            seleccionarTab(btnPendientes, botones)
            if (userRol == "tecnico") cargarSolicitudesTecnico("PENDIENTES") else cargarSolicitudesCliente("ACTIVOS")
        }
        btnEnCurso?.setOnClickListener { 
            seleccionarTab(btnEnCurso, botones)
            if (userRol == "tecnico") cargarSolicitudesTecnico("EN CURSO") else cargarSolicitudesCliente("ACTIVOS")
        }
        btnFinalizados?.setOnClickListener { 
            seleccionarTab(btnFinalizados, botones)
            if (userRol == "tecnico") cargarSolicitudesTecnico("FINALIZADOS") else cargarSolicitudesCliente("HISTORIAL")
        }
    }

    private fun seleccionarTab(seleccionado: Button, todos: List<Button?>) {
        todos.forEach {
            it?.setBackgroundResource(android.R.color.transparent)
            it?.setTextColor(getColor(R.color.chambita_texto_secundario))
        }
        seleccionado.setBackgroundResource(R.drawable.bg_tab_activo)
        seleccionado.setTextColor(getColor(android.R.color.white))
    }

    private fun cargarSolicitudesTecnico(tab: String) {
        solicitudesListener?.remove()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        var query: Query = db.collection("solicitudes")

        when (tab) {
            "PENDIENTES" -> {
                if (misDistritos.isNotEmpty()) {
                    query = query.whereEqualTo("estado", "pendiente")
                                 .whereIn("distritoServicio", misDistritos)
                }
            }
            "EN CURSO" -> {
                query = query.whereEqualTo("tecnicoId", uid)
                             .whereIn("estado", listOf("aceptada", "en_curso"))
            }
            "FINALIZADOS" -> {
                query = query.whereEqualTo("tecnicoId", uid)
                             .whereIn("estado", listOf("finalizada", "cancelada"))
            }
        }

        solicitudesListener = query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            val lista = value?.toObjects(Solicitud::class.java) ?: emptyList()
            adapterTecnico.updateList(lista)
            findViewById<LinearLayout>(R.id.layoutVacio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun cargarSolicitudesCliente(tab: String) {
        solicitudesListener?.remove()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        var query = db.collection("solicitudes").whereEqualTo("clienteId", uid)

        if (tab == "ACTIVOS") {
            query = query.whereIn("estado", listOf("pendiente", "aceptada", "en_curso"))
        } else {
            query = query.whereIn("estado", listOf("finalizada", "cancelada"))
        }

        solicitudesListener = query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            val lista = value?.toObjects(Solicitud::class.java) ?: emptyList()
            adapterCliente.updateList(lista)
            findViewById<LinearLayout>(R.id.layoutVacio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun aceptarTrabajo(solicitudId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        solicitudRepo.aceptarSolicitud(solicitudId, uid) {
            showToast("¡Trabajo aceptado!")
            cargarSolicitudesTecnico("EN CURSO")
        }
    }

    private fun finalizarTrabajo(solicitudId: String) {
        solicitudRepo.finalizarSolicitud(solicitudId) {
            showToast("¡Trabajo finalizado! El cliente podrá calificarte.")
            cargarSolicitudesTecnico("FINALIZADOS")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        solicitudesListener?.remove()
    }
}
