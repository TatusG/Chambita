package com.chambita.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Solicitud
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MisSolicitudesActivity : NavActivity() {

    private lateinit var adapter: SolicitudAdapter
    private val listaSolicitudes = mutableListOf<Solicitud>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_solicitudes)

        barraNavegacion()
        setupRecyclerView()
        setupFiltros()
        
        cargarSolicitudes("pendiente")
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvSolicitudes)
        adapter = SolicitudAdapter(listaSolicitudes) { solicitud ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("tecnicoId", solicitud.tecnicoId)
            startActivity(intent)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupFiltros() {
        val btnPendientes = findViewById<Button>(R.id.btnPendientes)
        val btnEnCurso = findViewById<Button>(R.id.btnEnCurso)
        val btnFinalizados = findViewById<Button>(R.id.btnFinalizados)
        val botones = listOf(btnPendientes, btnEnCurso, btnFinalizados)

        btnPendientes?.setOnClickListener { 
            seleccionarTab(btnPendientes, botones)
            cargarSolicitudes("pendiente") 
        }
        btnEnCurso?.setOnClickListener { 
            seleccionarTab(btnEnCurso, botones)
            cargarSolicitudes("en_curso") 
        }
        btnFinalizados?.setOnClickListener { 
            seleccionarTab(btnFinalizados, botones)
            cargarSolicitudes("finalizada") 
        }
    }

    private fun seleccionarTab(seleccionado: Button, todos: List<Button?>) {
        todos.forEach {
            it?.setBackgroundColor(Color.TRANSPARENT)
            it?.setTextColor(Color.parseColor("#6B7280"))
        }
        seleccionado.setBackgroundResource(R.drawable.bg_tab_activo)
        seleccionado.setTextColor(Color.WHITE)
    }

    private fun cargarSolicitudes(estado: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("solicitudes")
            .whereEqualTo("clienteId", uid)
            .whereEqualTo("estado", estado)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", "Error en la consulta: ${error.message}")
                    // MOSTRAR ERROR AL USUARIO SI FALTA EL INDICE
                    if (error.message?.contains("index") == true) {
                        Toast.makeText(this, "Falta crear un índice en Firebase. Revisa Logcat.", Toast.LENGTH_LONG).show()
                    }
                    return@addSnapshotListener
                }
                
                listaSolicitudes.clear()
                value?.documents?.forEach { doc ->
                    try {
                        val solicitud = doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                        if (solicitud != null) {
                            listaSolicitudes.add(solicitud)
                        }
                    } catch (e: Exception) {
                        Log.e("MAPPING_ERROR", "Error al convertir documento: ${e.message}")
                    }
                }
                
                findViewById<LinearLayout>(R.id.layoutVacio)?.visibility = 
                    if (listaSolicitudes.isEmpty()) View.VISIBLE else View.GONE
                
                adapter.notifyDataSetChanged()
            }
    }
}
