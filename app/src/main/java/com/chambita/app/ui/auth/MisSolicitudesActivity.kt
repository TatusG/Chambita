package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import java.util.Locale

class MisSolicitudesActivity : NavActivity() {

    private val TAG = "MIS_SOLICITUDES"
    private lateinit var adapterTecnico: SolicitudGestionTecnicoAdapter
    private lateinit var adapterCliente: SolicitudAdapter
    private val solicitudRepo = SolicitudRepository()
    private var solicitudesListener: ListenerRegistration? = null
    
    private var userRol: String = "cliente"
    private var misDistritos: List<String> = emptyList()
    private var tabActual: String = "PENDIENTES"

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
            onRechazar = { _ -> showToast("Solicitud omitida") },
            onChat = { solicitud -> abrirChat(solicitud.clienteId) },
            onFinalizar = { solicitud -> mostrarDialogoPago(solicitud) },
            onDetail = { solicitud -> mostrarDetalleSolicitud(solicitud) } // ✅ Mostrar detalle
        )
        
        adapterCliente = SolicitudAdapter(emptyList()) { solicitud, accion ->
            when (accion) {
                "CHAT" -> abrirChat(solicitud.tecnicoId ?: "")
                "CALIFICAR" -> {
                    val intent = Intent(this, CalificarServicioActivity::class.java).apply {
                        putExtra("solicitudId", solicitud.id)
                        putExtra("tecnicoId", solicitud.tecnicoId)
                    }
                    startActivity(intent)
                }
            }
        }

        findViewById<ImageView>(R.id.btnVolver)?.setOnClickListener { 
            onBackPressedDispatcher.onBackPressed()
        }
        setupFiltros()
    }

    private fun mostrarDetalleSolicitud(solicitud: Solicitud) {
        AlertDialog.Builder(this)
            .setTitle("Detalle del Servicio")
            .setMessage("Cliente: ${solicitud.nombreCliente}\n" +
                    "Distrito: ${solicitud.distritoServicio}\n" +
                    "Dirección: ${solicitud.direccionServicio}\n\n" +
                    "Descripción de la avería:\n${solicitud.descripcionAveria}")
            .setPositiveButton("Cerrar", null)
            .show()
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
            tabActual = "FINALIZADOS"
            cargarSolicitudesTecnico(tabActual)
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
                        tabActual = "PENDIENTES"
                        cargarSolicitudesTecnico(tabActual)
                    }
            } else {
                findViewById<RecyclerView>(R.id.rvSolicitudes).adapter = adapterCliente
                tabActual = "PENDIENTES"
                cargarSolicitudesCliente(tabActual)
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
            tabActual = "PENDIENTES"
            if (userRol == "tecnico") cargarSolicitudesTecnico(tabActual) else cargarSolicitudesCliente(tabActual)
        }
        btnEnCurso?.setOnClickListener { 
            seleccionarTab(btnEnCurso, botones)
            tabActual = "EN CURSO"
            if (userRol == "tecnico") cargarSolicitudesTecnico(tabActual) else cargarSolicitudesCliente(tabActual)
        }
        btnFinalizados?.setOnClickListener { 
            seleccionarTab(btnFinalizados, botones)
            tabActual = if (userRol == "tecnico") "FINALIZADOS" else "HISTORIAL"
            if (userRol == "tecnico") cargarSolicitudesTecnico(tabActual) else cargarSolicitudesCliente(tabActual)
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

        val mensajeVacio = when (tab) {
            "PENDIENTES" -> "No hay solicitudes pendientes"
            "EN CURSO" -> "No tienes trabajos en curso"
            "FINALIZADOS" -> "No hay trabajos finalizados"
            else -> "Sin datos"
        }

        if (tab == "PENDIENTES") {
            // Usar la lógica centralizada del repositorio para PENDIENTES
            solicitudesListener = solicitudRepo.escucharSolicitudesParaTecnico(uid, misDistritos) { lista ->
                adapterTecnico.updateList(lista)
                findViewById<TextView>(R.id.txtMensajeVacio)?.text = mensajeVacio
                findViewById<LinearLayout>(R.id.layoutVacio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
            return
        }

        var query: Query = db.collection("solicitudes")

        when (tab) {
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
                if (error != null) {
                    Log.e(TAG, "Error escuchando solicitudes: ${error.message}")
                    return@addSnapshotListener
                }
                val lista = value?.documents?.mapNotNull { doc ->
                    doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.fechaCreacion } ?: emptyList()
                
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
            // ✅ Mapeo de Document ID y ordenamiento en memoria
            val lista = value?.documents?.mapNotNull { doc ->
                doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
            }?.sortedByDescending { it.fechaCreacion } ?: emptyList()
            
            adapterCliente.updateList(lista)
            findViewById<TextView>(R.id.txtMensajeVacio)?.text = mensajeVacio
            findViewById<LinearLayout>(R.id.layoutVacio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun aceptarTrabajo(solicitudId: String) {
        if (solicitudId.isEmpty()) {
            showToast("Error: ID de solicitud no válido")
            return
        }
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        solicitudRepo.aceptarSolicitud(solicitudId, uid) {
            showToast("¡Trabajo aceptado!")
            if (tabActual == "PENDIENTES") {
                tabActual = "EN CURSO"
                val btnEnCurso = findViewById<Button>(R.id.btnEnCurso)
                val botones = listOf(findViewById<Button>(R.id.btnPendientes), btnEnCurso, findViewById<Button>(R.id.btnFinalizados))
                seleccionarTab(btnEnCurso, botones)
            }
            cargarSolicitudesTecnico(tabActual)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        solicitudesListener?.remove()
    }
}
