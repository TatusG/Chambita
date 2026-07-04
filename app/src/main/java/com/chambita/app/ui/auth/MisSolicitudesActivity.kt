package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.data.local.AppDatabase
import com.chambita.app.data.repository.SolicitudRepository
import com.chambita.app.models.Solicitud
import com.chambita.app.models.Pago
import com.google.firebase.Timestamp
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
            onFinalizar = { solicitud -> mostrarDialogoPago(solicitud) }
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

        findViewById<ImageView>(R.id.btnVolver)?.setOnClickListener { 
            onBackPressedDispatcher.onBackPressed()
        }
        setupFiltros()
    }

    private fun mostrarDialogoPago(solicitud: Solicitud) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_registrar_pago, null)
        val etMonto = view.findViewById<EditText>(R.id.etMontoCobrado)
        val rgMetodo = view.findViewById<RadioGroup>(R.id.rgMetodoPago)

        AlertDialog.Builder(this)
            .setTitle("Finalizar y Registrar Pago")
            .setView(view)
            .setPositiveButton("Confirmar") { _, _ ->
                val monto = etMonto.text.toString().toDoubleOrNull() ?: 0.0
                val selectedId = rgMetodo.checkedRadioButtonId
                val metodo = when(selectedId) {
                    R.id.rbYape -> "Yape"
                    R.id.rbPlin -> "Plin"
                    else -> "Efectivo"
                }
                
                if (monto > 0) {
                    finalizarTrabajoConPago(solicitud, monto, metodo)
                } else {
                    showToast("Ingresa un monto válido")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarTrabajoConPago(solicitud: Solicitud, monto: Double, metodo: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val nuevoPago = Pago(
            clienteId = solicitud.clienteId,
            tecnicoId = uid,
            solicitudId = solicitud.id,
            monto = monto,
            metodoUsado = metodo,
            fechaRegistro = Timestamp.now()
        )

        val batch = db.batch()
        
        val pagoRef = db.collection("pagos").document()
        batch.set(pagoRef, nuevoPago)

        val solicitudRef = db.collection("solicitudes").document(solicitud.id)
        batch.update(solicitudRef, "estado", "finalizada", "montoFinal", monto)

        batch.commit().addOnSuccessListener {
            showToast("¡Trabajo finalizado y pago registrado!")
            cargarSolicitudesTecnico("FINALIZADOS")
        }
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
                cargarSolicitudesCliente("PENDIENTES")
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
            if (userRol == "tecnico") cargarSolicitudesTecnico("PENDIENTES") else cargarSolicitudesCliente("PENDIENTES")
        }
        btnEnCurso?.setOnClickListener { 
            seleccionarTab(btnEnCurso, botones)
            if (userRol == "tecnico") cargarSolicitudesTecnico("EN CURSO") else cargarSolicitudesCliente("EN CURSO")
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

        val mensajeVacio = when (tab) {
            "PENDIENTES" -> "No hay solicitudes pendientes"
            "EN CURSO" -> "No tienes trabajos en curso"
            "FINALIZADOS" -> "No hay trabajos finalizados"
            else -> "Sin datos"
        }

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
            findViewById<TextView>(R.id.txtMensajeVacio)?.text = mensajeVacio
            findViewById<LinearLayout>(R.id.layoutVacio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun cargarSolicitudesCliente(tab: String) {
        solicitudesListener?.remove()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        var query = db.collection("solicitudes").whereEqualTo("clienteId", uid)

        val mensajeVacio = when (tab) {
            "PENDIENTES" -> "No tienes solicitudes pendientes"
            "EN CURSO" -> "No tienes servicios en curso"
            "HISTORIAL" -> "Tu historial está vacío"
            else -> "Sin datos"
        }

        when (tab) {
            "PENDIENTES" -> {
                query = query.whereEqualTo("estado", "pendiente")
            }
            "EN CURSO" -> {
                query = query.whereIn("estado", listOf("aceptada", "en_curso"))
            }
            "HISTORIAL" -> {
                query = query.whereIn("estado", listOf("finalizada", "cancelada"))
            }
        }

        solicitudesListener = query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            val lista = value?.toObjects(Solicitud::class.java) ?: emptyList()
            adapterCliente.updateList(lista)
            findViewById<TextView>(R.id.txtMensajeVacio)?.text = mensajeVacio
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

    override fun onDestroy() {
        super.onDestroy()
        solicitudesListener?.remove()
    }
}
