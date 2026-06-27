package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Usuario

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
        private val tvDistritos: TextView = itemView.findViewById(R.id.tvDistritos)
        private val tvTarifa: TextView = itemView.findViewById(R.id.tvTarifa)
        private val tvDisponibilidad: TextView = itemView.findViewById(R.id.tvDisponibilidad)

        fun bind(tecnico: Usuario, onItemClick: (Usuario) -> Unit) {
            // Asignar textos básicos
            tvNombre.text = tecnico.nombreCompleto.ifEmpty { "Técnico Asociado" }
            tvEspecialidad.text = tecnico.especialidad.ifEmpty { "Servicios Generales" }
            
            // Calificación promedio y cantidad de reseñas
            val promedio = if (tecnico.promedioEstrellas > 0) String.format("%.1f", tecnico.promedioEstrellas) else "N/A"
            tvCalificacion.text = "$promedio (${tecnico.numeroResenas} reseñas)"

            // Distritos que cubre
            val distritosTexto = if (tecnico.distritos.isNotEmpty()) {
                tecnico.distritos.joinToString(", ")
            } else {
                "No especificado"
            }
            tvDistritos.text = "Cubre: $distritosTexto"

            // Tarifa formateada
            tvTarifa.text = "S/. ${String.format("%.2f", tecnico.tarifaPorHora)}"

            // Configurar badge de disponibilidad y su color
            if (tecnico.disponible) {
                tvDisponibilidad.text = "Disponible"
                tvDisponibilidad.setTextColor(ContextCompat.getColor(itemView.context, R.color.chambita_verde))
                tvDisponibilidad.setBackgroundResource(R.drawable.bg_pill_white) // Fondo claro
            } else {
                tvDisponibilidad.text = "Ocupado"
                tvDisponibilidad.setTextColor(ContextCompat.getColor(itemView.context, R.color.chambita_texto_secundario))
                tvDisponibilidad.setBackgroundResource(R.drawable.bg_pill_rating) // Fondo gris
            }

            // Imagen de perfil por defecto (temporalmente, ya que no usamos Glide de terceros para evitar fallos de internet)
            imgFotoPerfil.setImageResource(R.drawable.ic_usuario)

            // Click listener
            itemView.setOnClickListener {
                onItemClick(tecnico)
            }
        }
    }
}
