package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Direccion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MisDireccionesActivity : NavActivity() {

    private lateinit var rvDirecciones: RecyclerView
    private lateinit var adapter: DireccionAdapter
    private var canAddMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_direcciones)

        barraNavegacion()
        
        inicializarComponentes()
        
        findViewById<LinearLayout>(R.id.btnVolver)?.setOnClickListener { finish() }
        
        findViewById<LinearLayout>(R.id.btnAgregarDireccion)?.setOnClickListener {
            if (canAddMore) {
                startActivity(Intent(this, AgregarDireccionActivity::class.java))
            } else {
                showToast("Máximo 5 direcciones permitidas")
            }
        }

        cargarMapaEstatico()
    }

    private fun cargarMapaEstatico() {
        val imgMapa = findViewById<ImageView>(R.id.imgMapa) ?: return
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val distrito = doc.getString("distritoResidencia") ?: "Lima,Peru"
                
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val apiKey = com.chambita.app.BuildConfig.MAPS_API_KEY
                        if (apiKey.isEmpty()) return@launch

                        val center = "${distrito.replace(" ", "+")},+Lima,+Peru"
                        val urlString = "https://maps.googleapis.com/maps/api/staticmap" +
                                "?center=$center&zoom=14&size=600x350&scale=2&maptype=roadmap&key=$apiKey"

                        val url = URL(urlString)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.setRequestProperty("X-Android-Package", packageName)

                        if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                            val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                            withContext(Dispatchers.Main) {
                                imgMapa.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MAPS", "Error en mapa direcciones", e)
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        cargarDirecciones()
    }

    private fun inicializarComponentes() {
        rvDirecciones = findViewById(R.id.rvDirecciones)
        rvDirecciones.layoutManager = LinearLayoutManager(this)
        adapter = DireccionAdapter(emptyList()) { direccion ->
            showToast("Editar ${direccion.alias}")
        }
        rvDirecciones.adapter = adapter
    }

    private fun cargarDirecciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).collection("direcciones")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.toObjects(Direccion::class.java)
                adapter.updateList(lista)
                
                canAddMore = lista.size < 5
                
                if (lista.isEmpty()) {
                    showToast("No tienes direcciones guardadas")
                }
            }
    }
}
