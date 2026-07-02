package com.chambita.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Resena
import com.chambita.app.models.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class VistaClientePerfilTecnicoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var tecnicoUid: String? = null
    private var tecnicoNombre: String = ""
    private var tecnicoTelefono: String = ""
    private var tecnicoEspecialidad: String = ""

    private lateinit var tvAvatarTecnico: TextView
    private lateinit var tvNombreTecnico: TextView
    private lateinit var tvEspecialidad: TextView
    private lateinit var tvZonaAtencion: TextView
    private lateinit var ratingBarTecnico: RatingBar
    private lateinit var tvRatingNumero: TextView
    private lateinit var tvNumTrabajos: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvTarifa: TextView
    private lateinit var llChipsServicios: LinearLayout
    private lateinit var rvResenas: RecyclerView
    private lateinit var resenaAdapter: ResenaInnerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vista_cliente_perfil_tecnico)

        db = FirebaseFirestore.getInstance()
        tecnicoUid = intent.getStringExtra("tecnicoUid")

        if (tecnicoUid == null) {
            Toast.makeText(this, "Error al cargar el perfil del técnico", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarComponentes()
        configurarListeners()
        cargarDatosPerfil()
        cargarResenas()
    }

    private fun inicializarComponentes() {
        tvAvatarTecnico = findViewById(R.id.tvAvatarTecnico)
        tvNombreTecnico = findViewById(R.id.tvNombreTecnico)
        tvEspecialidad = findViewById(R.id.tvEspecialidad)
        tvZonaAtencion = findViewById(R.id.tvZonaAtencion)
        ratingBarTecnico = findViewById(R.id.ratingBarTecnico)
        tvRatingNumero = findViewById(R.id.tvRatingNumero)
        tvNumTrabajos = findViewById(R.id.tvNumTrabajos)
        tvEstado = findViewById(R.id.tvEstado)
        tvTarifa = findViewById(R.id.tvTarifa)
        llChipsServicios = findViewById(R.id.llChipsServicios)
        rvResenas = findViewById(R.id.rvResenas)

        // Configurar RecyclerView de Reseñas
        rvResenas.layoutManager = LinearLayoutManager(this)
        resenaAdapter = ResenaInnerAdapter(emptyList())
        rvResenas.adapter = resenaAdapter
    }

    private fun configurarListeners() {
        findViewById<ImageButton>(R.id.btnVolver)?.setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnNotificaciones)?.setOnClickListener {
            Toast.makeText(this, "No tienes notificaciones nuevas", Toast.LENGTH_SHORT).show()
        }

        // Llamar por teléfono
        findViewById<LinearLayout>(R.id.btnLlamar)?.setOnClickListener {
            if (tecnicoTelefono.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tecnicoTelefono"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Número de teléfono no disponible", Toast.LENGTH_SHORT).show()
            }
        }

        // Abrir ventana de chat
        findViewById<LinearLayout>(R.id.btnChat)?.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("tecnicoId", tecnicoUid)
                putExtra("nombreTecnico", tecnicoNombre)
            }
            startActivity(intent)
        }

        // Contratar (Crear nueva solicitud)
        findViewById<Button>(R.id.btnContratar)?.setOnClickListener {
            val intent = Intent(this, NuevaSolicitudActivity::class.java).apply {
                putExtra("tecnicoId", tecnicoUid)
                putExtra("nombreTecnico", tecnicoNombre)
                putExtra("tarifa", tvTarifa.text.toString().replace("S/. ", "").replace(",", ".").toDoubleOrNull() ?: 0.0)
            }
            startActivity(intent)
        }

        // Ver en mapa (simulado)
        findViewById<LinearLayout>(R.id.btnVerMapa)?.setOnClickListener {
            Toast.makeText(this, "Cobertura del técnico: $tecnicoEspecialidad en Lima Metropolitana", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatosPerfil() {
        db.collection("usuarios").document(tecnicoUid!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val tecnico = document.toObject(Usuario::class.java)
                    if (tecnico != null) {
                        tecnicoNombre = tecnico.nombreCompleto
                        tecnicoTelefono = tecnico.telefono
                        tecnicoEspecialidad = tecnico.especialidad

                        // Nombre y Especialidad
                        tvNombreTecnico.text = tecnico.nombreCompleto
                        tvEspecialidad.text = tecnico.especialidad.uppercase()

                        // Iniciales para el avatar circular
                        tvAvatarTecnico.text = obtenerIniciales(tecnico.nombreCompleto)

                        // Zonas/Distritos de atención
                        val distritosTexto = if (tecnico.distritos.isNotEmpty()) {
                            tecnico.distritos.joinToString(", ")
                        } else {
                            "Lima Metropolitana"
                        }
                        tvZonaAtencion.text = distritosTexto

                        // Calificación
                        ratingBarTecnico.rating = tecnico.promedioEstrellas.toFloat()
                        tvRatingNumero.text = "${String.format("%.1f", tecnico.promedioEstrellas)} (${tecnico.numeroResenas} reseñas)"

                        // Métricas
                        tvNumTrabajos.text = tecnico.conteoTrabajos.toString()
                        
                        if (tecnico.disponible) {
                            tvEstado.text = "Disponible"
                            tvEstado.setTextColor(getColor(R.color.chambita_verde))
                        } else {
                            tvEstado.text = "Ocupado"
                            tvEstado.setTextColor(getColor(R.color.chambita_texto_secundario))
                        }

                        tvTarifa.text = "S/. ${String.format("%.2f", tecnico.tarifaPorHora)}"

                        // Cargar subcategorías/servicios como chips
                        llChipsServicios.removeAllViews()
                        tecnico.servicios.forEach { servicio ->
                            agregarChipServicio(servicio)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("PERFIL_TECNICO", "Error al cargar perfil de técnico", exception)
            }
    }

    private fun cargarResenas() {
        db.collection("usuarios").document(tecnicoUid!!).collection("resenas")
            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listaResenas = mutableListOf<Resena>()
                for (doc in querySnapshot) {
                    val resena = doc.toObject(Resena::class.java)
                    listaResenas.add(resena)
                }
                resenaAdapter.actualizarResenas(listaResenas)
            }
            .addOnFailureListener { e ->
                Log.e("PERFIL_TECNICO", "Error al cargar reseñas del técnico", e)
            }
    }

    private fun obtenerIniciales(nombreCompleto: String): String {
        val partes = nombreCompleto.trim().split("\\s+".toRegex())
        return when {
            partes.isEmpty() -> "T"
            partes.size == 1 -> partes[0].take(2).uppercase()
            else -> (partes[0].take(1) + partes[1].take(1)).uppercase()
        }
    }

    private fun agregarChipServicio(nombreServicio: String) {
        val tvChip = TextView(this)
        tvChip.text = nombreServicio
        tvChip.setPadding(32, 12, 32, 12)
        tvChip.background = ContextCompat.getDrawable(this, R.drawable.bg_pill_white)
        tvChip.setTextColor(ContextCompat.getColor(this, R.color.chambita_texto_principal))
        tvChip.textSize = 14f
        
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 16, 0)
        }
        tvChip.layoutParams = params
        llChipsServicios.addView(tvChip)
    }

    // Toast de utilidad
    private fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    // Adapter interno para las opiniones/reseñas del técnico
    private class ResenaInnerAdapter(private var list: List<Resena>) : RecyclerView.Adapter<ResenaInnerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resena, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val resena = list[position]
            holder.tvNombre.text = resena.nombreCliente.ifEmpty { "Cliente Anónimo" }
            holder.tvCalificacion.text = resena.calificacion.toString()
            holder.tvComentario.text = resena.comentario.ifEmpty { "Sin comentario" }
            
            if (resena.recomienda) {
                holder.tvRecomienda.text = "✓ Recomienda a este técnico"
                holder.tvRecomienda.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.chambita_verde))
            } else {
                holder.tvRecomienda.text = "✗ No recomienda a este técnico"
                holder.tvRecomienda.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.chambita_rojo))
            }

            val fecha = resena.fechaRegistro
            if (fecha != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                holder.tvFecha.text = sdf.format(fecha.toDate())
            } else {
                holder.tvFecha.text = ""
            }
        }

        override fun getItemCount(): Int = list.size

        fun actualizarResenas(newList: List<Resena>) {
            list = newList
            notifyDataSetChanged()
        }

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombreCliente)
            val tvCalificacion: TextView = v.findViewById(R.id.tvCalificacion)
            val tvComentario: TextView = v.findViewById(R.id.tvComentario)
            val tvRecomienda: TextView = v.findViewById(R.id.tvRecomienda)
            val tvFecha: TextView = v.findViewById(R.id.tvFecha)
        }
    }
}
