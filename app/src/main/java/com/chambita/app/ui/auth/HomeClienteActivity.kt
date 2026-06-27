package com.chambita.app.ui.auth

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class HomeClienteActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cliente)

        // Activar navegación inferior
        barraNavegacion()
        cargarDatosCabecera()

        // Configurar botones de la cabecera
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            showToast("Menú lateral próximamente")
        }

        findViewById<ImageButton>(R.id.btnNotificaciones)?.setOnClickListener {
            showToast("No tienes notificaciones nuevas")
        }

        findViewById<ImageView>(R.id.imgPerfil)?.setOnClickListener {
            startActivity(Intent(this, PerfilClienteActivity::class.java))
        }

        // Configurar búsqueda y mapa
        findViewById<EditText>(R.id.etBuscar)?.setOnEditorActionListener { v, _, _ ->
            showToast("Buscando: ${v.text}")
            false
        }

        findViewById<CardView>(R.id.cardMapa)?.setOnClickListener {
            startActivity(Intent(this, NuevaSolicitudActivity::class.java))
        }
    }

    private fun cargarDatosCabecera() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombreCompleto = document.getString("nombreCompleto") ?: "Usuario"
                    // Tomamos solo el primer nombre para el saludo
                    val primerNombre = nombreCompleto.split(" ")[0]
                    
                    findViewById<TextView>(R.id.tvSaludo)?.text = "HOLA, ${primerNombre.uppercase()}"

                    // Obtenemos el distrito de residencia del cliente para el mapa estático
                    val distrito = document.getString("distritoResidencia") ?: "Ventanilla"
                    cargarMapaEstatico(distrito)
                }
            }
    }

    private fun cargarMapaEstatico(distrito: String) {
        val imgMapa = findViewById<ImageView>(R.id.imgMapa) ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtenemos la clave de API desde BuildConfig (inyectada por el Secrets Gradle Plugin)
                val apiKey = com.chambita.app.BuildConfig.MAPS_API_KEY
                Log.d("MAPS", "API Key obtenida: $apiKey")

                if (apiKey.isEmpty() || apiKey.startsWith("AIza") == false) {
                    Log.e("MAPS", "Clave API de mapas ausente o con formato inválido en BuildConfig")
                    return@launch
                }

                // Generamos la URL del Static Maps API de Google
                val center = "${distrito.replace(" ", "+")},+Lima,+Peru"
                val urlString = "https://maps.googleapis.com/maps/api/staticmap" +
                        "?center=$center" +
                        "&zoom=14" +
                        "&size=600x300" +
                        "&scale=2" +
                        "&maptype=roadmap" +
                        "&key=$apiKey"

                Log.d("MAPS", "Intentando cargar mapa desde URL: $urlString")

                val url = URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode
                Log.d("MAPS", "Código de respuesta HTTP: $responseCode")

                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            imgMapa.setImageBitmap(bitmap)
                            imgMapa.scaleType = ImageView.ScaleType.CENTER_CROP
                            Log.d("MAPS", "Mapa cargado exitosamente en la vista")
                        } else {
                            Log.e("MAPS", "El stream decodificó un bitmap nulo")
                        }
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: "Sin detalles"
                    Log.e("MAPS", "Error de Google Maps API ($responseCode): $errorText")
                }
            } catch (e: Exception) {
                Log.e("MAPS", "Excepción al descargar el mapa estático", e)
            }
        }
    }
}

