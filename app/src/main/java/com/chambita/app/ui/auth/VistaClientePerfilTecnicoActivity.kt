package com.chambita.app.ui.auth

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Resena
import com.chambita.app.models.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class VistaClientePerfilTecnicoActivity : NavActivity() {

    private lateinit var db: FirebaseFirestore
    private var tecnicoUid: String? = null
    private var tecnicoNombre: String = ""
    private var tecnicoTelefono: String = ""
    private var tecnicoEspecialidad: String = ""
    private var tecnicoData: Usuario? = null

    private lateinit var tvAvatarTecnico: TextView
    private lateinit var tvNombreTecnico: TextView
    private lateinit var tvEspecialidad: TextView
    private lateinit var tvZonaAtencion: TextView
    private lateinit var ratingBarTecnico: RatingBar
    private lateinit var tvRatingNumero: TextView
    private lateinit var tvNumTrabajos: TextView
    private lateinit var tvEstado: TextView
    private lateinit var viewStatusDot: View
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
            showToast("Error al cargar el perfil del técnico")
            finish()
            return
        }

        barraNavegacion()
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
        viewStatusDot = findViewById(R.id.viewStatusDot)
        tvTarifa = findViewById(R.id.tvTarifa)
        llChipsServicios = findViewById(R.id.llChipsServicios)
        rvResenas = findViewById(R.id.rvResenas)

        rvResenas.layoutManager = LinearLayoutManager(this)
        resenaAdapter = ResenaInnerAdapter(emptyList())
        rvResenas.adapter = resenaAdapter
    }

    private fun configurarListeners() {
        findViewById<ImageButton>(R.id.btnVolver)?.setOnClickListener { finish() }

        findViewById<Button>(R.id.btnLlamar)?.setOnClickListener {
            if (tecnicoTelefono.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tecnicoTelefono")))
            } else {
                showToast("Teléfono no disponible")
            }
        }

        findViewById<Button>(R.id.btnChat)?.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("tecnicoId", tecnicoUid)
                putExtra("nombreTecnico", tecnicoNombre)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnContratar)?.setOnClickListener {
            val intent = Intent(this, NuevaSolicitudActivity::class.java).apply {
                putExtra("tecnicoId", tecnicoUid)
                putExtra("nombreTecnico", tecnicoNombre)
                putExtra("tarifa", tecnicoData?.tarifaPorHora ?: 0.0)
            }
            startActivity(intent)
        }
    }

    private fun cargarDatosPerfil() {
        db.collection("usuarios").document(tecnicoUid!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val tecnico = document.toObject(Usuario::class.java)
                    if (tecnico != null) {
                        tecnicoData = tecnico
                        tecnicoNombre = tecnico.nombreCompleto
                        tecnicoTelefono = tecnico.telefono
                        tecnicoEspecialidad = tecnico.especialidad

                        tvNombreTecnico.text = tecnico.nombreCompleto
                        tvEspecialidad.text = tecnico.especialidad.uppercase()
                        tvAvatarTecnico.text = obtenerIniciales(tecnico.nombreCompleto)
                        tvZonaAtencion.text = "Atiende: ${tecnico.distritos.joinToString(", ")}"
                        
                        ratingBarTecnico.rating = tecnico.promedioEstrellas.toFloat()
                        tvRatingNumero.text = String.format(Locale.getDefault(), "%.1f", tecnico.promedioEstrellas)

                        tvNumTrabajos.text = tecnico.conteoTrabajos.toString()
                        tvEstado.text = if (tecnico.disponible) "LIBRE" else "OCUPADO"
                        viewStatusDot.visibility = if (tecnico.disponible) View.VISIBLE else View.GONE
                        
                        tvTarifa.text = "S/ ${tecnico.tarifaPorHora.toInt()}"

                        llChipsServicios.removeAllViews()
                        tecnico.servicios.forEach { agregarChipServicio(it) }
                    }
                }
            }
    }

    private fun cargarResenas() {
        db.collection("usuarios").document(tecnicoUid!!).collection("resenas")
            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lista = querySnapshot.toObjects(Resena::class.java)
                resenaAdapter.actualizarResenas(lista)
            }
    }

    private fun obtenerIniciales(nombre: String): String {
        val partes = nombre.trim().split("\\s+".toRegex())
        return if (partes.size >= 2) {
            "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        } else {
            nombre.take(2).uppercase()
        }
    }

    private fun agregarChipServicio(nombre: String) {
        val tvChip = TextView(this)
        tvChip.text = nombre.uppercase()
        tvChip.setPadding(32, 12, 32, 12)
        tvChip.background = ContextCompat.getDrawable(this, R.drawable.bg_pill_grey)
        tvChip.setTextColor(ContextCompat.getColor(this, R.color.figma_grey))
        tvChip.textSize = 10f
        tvChip.setTypeface(null, Typeface.BOLD)
        
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 16, 0) }
        tvChip.layoutParams = params
        llChipsServicios.addView(tvChip)
    }

    private class ResenaInnerAdapter(private var list: List<Resena>) : RecyclerView.Adapter<ResenaInnerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resena, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvNombre.text = item.nombreCliente.ifEmpty { "Cliente" }
            holder.tvComentario.text = item.comentario
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            holder.tvFecha.text = item.fechaRegistro?.let { sdf.format(it.toDate()) } ?: ""
            holder.tvCalificacion.text = item.calificacion.toString()
            
            if (item.recomienda) {
                holder.tvRecomienda.text = "✓ Recomienda a este técnico"
                holder.tvRecomienda.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.chambita_verde))
            } else {
                holder.tvRecomienda.text = "✗ No recomienda a este técnico"
                holder.tvRecomienda.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.chambita_rojo))
            }
        }
        override fun getItemCount() = list.size
        fun actualizarResenas(newList: List<Resena>) {
            list = newList
            notifyDataSetChanged()
        }
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvNombreCliente)
            val tvComentario: TextView = v.findViewById(R.id.tvComentario)
            val tvFecha: TextView = v.findViewById(R.id.tvFecha)
            val tvCalificacion: TextView = v.findViewById(R.id.tvCalificacion)
            val tvRecomienda: TextView = v.findViewById(R.id.tvRecomienda)
        }
    }
}
