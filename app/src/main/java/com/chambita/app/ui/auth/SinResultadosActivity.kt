package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SinResultadosActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var distritoBuscado: String? = null
    private var listaVecinosGlobal: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sin_resultados)

        distritoBuscado = intent.getStringExtra("distrito") ?: "Ventanilla"
        
        barraNavegacion()
        inicializarComponentes()
        cargarDistritosVecinos()
    }

    private fun inicializarComponentes() {
        findViewById<ImageButton>(R.id.btnVolver)?.setOnClickListener { finish() }
        
        val tvDistrito = findViewById<TextView>(R.id.tvDistritoBuscado)
        tvDistrito?.text = "Sin técnicos en \"$distritoBuscado\""

        findViewById<Button>(R.id.btnBuscarVecinos)?.setOnClickListener {
            if (listaVecinosGlobal.isNotEmpty()) {
                volverAHomeYBuscar(listaVecinosGlobal)
            } else {
                showToast("No hay distritos vecinos configurados")
            }
        }

        findViewById<Button>(R.id.btnAmpliarBusqueda)?.setOnClickListener {
            volverAHomeYBuscar(null) // null significa ampliar a todo Lima
        }
    }

    private fun cargarDistritosVecinos() {
        lifecycleScope.launch {
            try {
                val docDistrito = db.collection("distritos").document(distritoBuscado!!).get().await()
                val vecinos = docDistrito.get("distritosVecinos") as? List<String> ?: emptyList()
                listaVecinosGlobal = vecinos

                val listaResultados = mutableListOf<DistritoVecino>()

                for (vecino in vecinos) {
                    val count = db.collection("usuarios")
                        .whereEqualTo("rol", "tecnico")
                        .whereEqualTo("disponible", true)
                        .whereArrayContains("distritos", vecino)
                        .get()
                        .await()
                        .size()
                    
                    listaResultados.add(DistritoVecino(vecino, count))
                }

                configurarRecyclerView(listaResultados)

            } catch (e: Exception) {
                Log.e("SIN_RESULTADOS", "Error cargando vecinos", e)
                showToast("Error al cargar sugerencias")
            }
        }
    }

    private fun configurarRecyclerView(lista: List<DistritoVecino>) {
        val rv = findViewById<RecyclerView>(R.id.rvDistritos)
        rv?.layoutManager = LinearLayoutManager(this)
        rv?.adapter = DistritoVecinoAdapter(lista) { distrito ->
            volverAHomeYBuscar(listOf(distrito))
        }
    }

    private fun volverAHomeYBuscar(distritos: List<String>?) {
        val intent = Intent(this, HomeClienteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (distritos != null) {
                putStringArrayListExtra("filtroDistritos", ArrayList(distritos))
            }
        }
        startActivity(intent)
        finish()
    }
}
