package com.chambita.app.ui.auth

import android.os.Bundle
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

        if (tecnicoId == null) {
            finish()
            return
        }

        inicializarComponentes()
        cargarDatosTecnico()
    }

    private fun inicializarComponentes() {
        findViewById<LinearLayout>(R.id.btnVolver)?.setOnClickListener { finish() }

        // Lógica de estrellas
        val estrellas = listOf(
            findViewById<ImageButton>(R.id.star1),
            findViewById<ImageButton>(R.id.star2),
            findViewById<ImageButton>(R.id.star3),
            findViewById<ImageButton>(R.id.star4),
            findViewById<ImageButton>(R.id.star5)
        )

        estrellas.forEachIndexed { index, star ->
            star.setOnClickListener {
                seleccionarEstrellas(index + 1, estrellas)
            }
        }

        // Lógica de recomendación
        val btnSi = findViewById<LinearLayout>(R.id.btnSi)
        val btnNo = findViewById<LinearLayout>(R.id.btnNo)

        btnSi.setOnClickListener { seleccionarRecomendacion(true) }
        btnNo.setOnClickListener { seleccionarRecomendacion(false) }

        findViewById<Button>(R.id.btnEnviarResena)?.setOnClickListener {
            enviarResena()
        }
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
        
        val tvValoracion = findViewById<TextView>(R.id.txtValoracion)
        tvValoracion.text = when(puntaje) {
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
        val btnSi = findViewById<LinearLayout>(R.id.btnSi)
        val btnNo = findViewById<LinearLayout>(R.id.btnNo)
        
        if (si) {
            btnSi.setBackgroundResource(R.drawable.bg_recomendar_si)
            btnNo.setBackgroundResource(R.drawable.bg_recomendar_normal)
        } else {
            btnSi.setBackgroundResource(R.drawable.bg_recomendar_normal)
            btnNo.setBackgroundResource(R.drawable.bg_recomendar_no)
        }
    }

    private fun cargarDatosTecnico() {
        db.collection("usuarios").document(tecnicoId!!).get().addOnSuccessListener { doc ->
            val tecnico = doc.toObject(Usuario::class.java)
            tecnico?.let {
                findViewById<TextView>(R.id.txtNombre).text = it.nombreCompleto
                findViewById<TextView>(R.id.txtServicio).text = it.especialidad
                val img = findViewById<ImageView>(R.id.imgTecnico)
                if (it.fotoPerfil.isNotEmpty()) {
                    Glide.with(this).load(it.fotoPerfil).circleCrop().into(img)
                }
            }
        }
    }

    private fun enviarResena() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val comentario = findViewById<EditText>(R.id.etComentario)?.text.toString().trim()

        if (ratingSeleccionado == 0) {
            showToast("Por favor, selecciona una calificación")
            return
        }

        val resena = Resena(
            clienteId = uid,
            calificacion = ratingSeleccionado,
            recomienda = recomiendaSeleccionado,
            solicitudId = solicitudId ?: "",
            fechaRegistro = Timestamp.now()
        )

        db.collection("usuarios").document(tecnicoId!!).collection("resenas")
            .add(resena)
            .addOnSuccessListener {
                actualizarEstadoSolicitud()
            }
    }

    private fun actualizarEstadoSolicitud() {
        db.collection("solicitudes").document(solicitudId!!)
            .update("resenaDejada", true)
            .addOnSuccessListener {
                showToast("¡Gracias por tu reseña!")
                finish()
            }
    }
}
