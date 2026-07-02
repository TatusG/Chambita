package com.chambita.app.ui.auth

import android.os.Bundle
import android.util.Log
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
    }

    private fun cargarDistritosVecinos() {
        lifecycleScope.launch {
            try {
                // 1. Leer documento del distrito para obtener vecinos
                val docDistrito = db.collection("distritos").document(distritoBuscado!!).get().await()
                val vecinos = docDistrito.get("distritosVecinos") as? List<String> ?: emptyList()

                val listaResultados = mutableListOf<DistritoVecino>()

                // 2. Por cada vecino, contar técnicos disponibles
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

                // 3. Poblar RecyclerView
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
            // Al hacer clic en un vecino, podríamos volver al home filtrando por ese distrito
            showToast("Buscando en $distrito...")
        }
    }
}
