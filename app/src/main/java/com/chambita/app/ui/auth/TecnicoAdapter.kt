package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Usuario
import java.util.Locale

class TecnicoAdapter(
    private var listaTecnicos: List<Usuario>,
    private val onItemClick: (Usuario) -> Unit
) : RecyclerView.Adapter<TecnicoAdapter.TecnicoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TecnicoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tecnico, parent, false)
        return TecnicoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TecnicoViewHolder, position: Int) {
        val tecnico = listaTecnicos[position]
        holder.bind(tecnico, onItemClick)
    }

    override fun getItemCount(): Int = listaTecnicos.size

    fun actualizarLista(nuevaLista: List<Usuario>) {
        listaTecnicos = nuevaLista
        notifyDataSetChanged()
    }

    class TecnicoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgFotoPerfil: ImageView = itemView.findViewById(R.id.imgFotoPerfil)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvEspecialidad: TextView = itemView.findViewById(R.id.tvEspecialidad)
        private val tvCalificacion: TextView = itemView.findViewById(R.id.tvCalificacion)
        private val tvDistritoBadge: TextView = itemView.findViewById(R.id.tvDistritoBadge)
        private val tvPrecioRango: TextView = itemView.findViewById(R.id.tvPrecioRango)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingMini)

        fun bind(tecnico: Usuario, onItemClick: (Usuario) -> Unit) {
            tvNombre.text = tecnico.nombreCompleto.ifEmpty { "Técnico Asociado" }
            tvEspecialidad.text = tecnico.especialidad.ifEmpty { "Servicios Generales" }
            
            // ✅ Mostrar promedio real o N/A si es 0
            if (tecnico.promedioEstrellas > 0) {
                tvCalificacion.text = String.format(Locale.US, "%.1f", tecnico.promedioEstrellas)
                ratingBar.rating = tecnico.promedioEstrellas.toFloat()
            } else {
                tvCalificacion.text = "N/A"
                ratingBar.rating = 0f
            }

            tvDistritoBadge.text = if (tecnico.distritos.isNotEmpty()) {
                tecnico.distritos[0].uppercase()
            } else {
                "LIMA"
            }

            // Mostrar tarifa
            tvPrecioRango.text = "S/ %.2f".format(tecnico.tarifaPorHora)

            imgFotoPerfil.setImageResource(R.drawable.ic_usuario)
            if (tecnico.fotoPerfil.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(tecnico.fotoPerfil)
                    .circleCrop()
                    .placeholder(R.drawable.ic_usuario)
                    .into(imgFotoPerfil)
            }

            itemView.setOnClickListener { onItemClick(tecnico) }
        }
    }
}
