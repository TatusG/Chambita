package com.chambita.app.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Resena
import com.chambita.app.models.Usuario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CalificarServicioActivity : NavActivity() {

    private val TAG = "CALIFICAR_LOG"
    private var solicitudId: String? = null
    private var tecnicoId: String? = null
    private val db = FirebaseFirestore.getInstance()
    
    private var ratingSeleccionado = 0
    private var recomiendaSeleccionado = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificar_servicio)

        solicitudId = intent.getStringExtra("solicitudId")
        tecnicoId = intent.getStringExtra("tecnicoId")

        Log.d(TAG, "Iniciando calificación. Solicitud: $solicitudId | Técnico: $tecnicoId")

        if (tecnicoId == null || solicitudId == null) {
            Log.e(TAG, "Error: IDs nulos")
            finish()
            return
        }

        inicializarComponentes()
        cargarDatosTecnico()
    }

    private fun inicializarComponentes() {
        findViewById<LinearLayout>(R.id.btnVolver)?.setOnClickListener { finish() }

        val estrellas = listOf(
            findViewById<ImageButton>(R.id.star1),
            findViewById<ImageButton>(R.id.star2),
            findViewById<ImageButton>(R.id.star3),
            findViewById<ImageButton>(R.id.star4),
            findViewById<ImageButton>(R.id.star5)
        )

        estrellas.forEachIndexed { index, star ->
            star.setOnClickListener { seleccionarEstrellas(index + 1, estrellas) }
        }

        findViewById<LinearLayout>(R.id.btnSi).setOnClickListener { seleccionarRecomendacion(true) }
        findViewById<LinearLayout>(R.id.btnNo).setOnClickListener { seleccionarRecomendacion(false) }

        findViewById<Button>(R.id.btnEnviarResena).setOnClickListener { enviarResena() }
    }

    private fun seleccionarEstrellas(puntaje: Int, lista: List<ImageButton>) {
        ratingSeleccionado = puntaje
        lista.forEachIndexed { index, star ->
            if (index < puntaje) {
                star.setColorFilter(ContextCompat.getColor(this, R.color.chambita_estrella))
            } else {
                star.setColorFilter(ContextCompat.getColor(this, R.color.chambita_texto_claro))
            }
        }
        findViewById<TextView>(R.id.txtValoracion).text = when(puntaje) {
            1 -> "Pésimo"
            2 -> "Regular"
            3 -> "Bueno"
            4 -> "Muy bueno"
            5 -> "¡Excelente!"
            else -> "Valoración"
        }
    }

    private fun seleccionarRecomendacion(si: Boolean) {
        recomiendaSeleccionado = si
        if (si) {
            findViewById<LinearLayout>(R.id.btnSi).setBackgroundResource(R.drawable.bg_recomendar_si)
            findViewById<LinearLayout>(R.id.btnNo).setBackgroundResource(R.drawable.bg_recomendar_normal)
        } else {
            findViewById<LinearLayout>(R.id.btnSi).setBackgroundResource(R.drawable.bg_recomendar_normal)
            findViewById<LinearLayout>(R.id.btnNo).setBackgroundResource(R.drawable.bg_recomendar_no)
        }
    }

    private fun cargarDatosTecnico() {
        db.collection("usuarios").document(tecnicoId!!).get().addOnSuccessListener { doc ->
            val tecnico = doc.toObject(Usuario::class.java)
            tecnico?.let {
                findViewById<TextView>(R.id.txtNombre).text = it.nombreCompleto
                findViewById<TextView>(R.id.txtServicio).text = it.especialidad
                if (it.fotoPerfil.isNotEmpty()) {
                    Glide.with(this).load(it.fotoPerfil).circleCrop().into(findViewById(R.id.imgTecnico))
                }
            }
        }
    }

    private fun enviarResena() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (ratingSeleccionado == 0) {
            showToast("Selecciona una calificación")
            return
        }

        Log.d(TAG, "Intentando enviar reseña...")

        val resena = Resena(
            clienteId = uid,
            nombreCliente = FirebaseAuth.getInstance().currentUser?.displayName ?: "Cliente",
            calificacion = ratingSeleccionado,
            recomienda = recomiendaSeleccionado,
            comentario = findViewById<EditText>(R.id.etComentario).text.toString().trim(),
            solicitudId = solicitudId ?: "",
            fechaRegistro = Timestamp.now()
        )

        db.runTransaction { transaction ->
            Log.d(TAG, "Ejecutando transacción Firestore")
            val tecnicoRef = db.collection("usuarios").document(tecnicoId!!)
            val snapshot = transaction.get(tecnicoRef)
            
            val numActual = snapshot.getLong("numeroResenas") ?: 0L
            val promActual = snapshot.getDouble("promedioEstrellas") ?: 0.0
            val conteoTrabajosActual = snapshot.getLong("conteoTrabajos") ?: 0L
            
            val nuevoNum = numActual + 1
            val nuevoProm = ((promActual * numActual) + ratingSeleccionado) / nuevoNum
            
            Log.d(TAG, "Nuevos valores -> Prom: $nuevoProm, Num: $nuevoNum")

            // Actualizamos el perfil del técnico directamente
            transaction.update(tecnicoRef, 
                "numeroResenas", nuevoNum,
                "promedioEstrellas", nuevoProm,
                "conteoTrabajos", conteoTrabajosActual + 1
            )
            
            // Guardamos la reseña
            val resenaRef = tecnicoRef.collection("resenas").document()
            transaction.set(resenaRef, resena)
            
            // Marcamos la solicitud como calificada
            val solicitudRef = db.collection("solicitudes").document(solicitudId!!)
            transaction.update(solicitudRef, "resenaDejada", true)
            
            null
        }.addOnSuccessListener {
            Log.d(TAG, "Transacción completada con éxito")
            showToast("¡Gracias por tu reseña!")
            finish()
        }.addOnFailureListener { e ->
            Log.e(TAG, "FALLO EN TRANSACCIÓN: ${e.message}", e)
            showToast("Error de permisos. Revisa el Logcat.")
        }
    }
}
