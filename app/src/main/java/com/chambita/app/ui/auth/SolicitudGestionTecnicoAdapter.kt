package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Solicitud
import java.text.SimpleDateFormat
import java.util.*

class SolicitudGestionTecnicoAdapter(
    private var list: List<Solicitud>,
    private val onAceptar: (Solicitud) -> Unit,
    private val onRechazar: (Solicitud) -> Unit,
    private val onChat: (Solicitud) -> Unit,
    private val onFinalizar: (Solicitud) -> Unit,
    private val onDetail: (Solicitud) -> Unit // ✅ Nuevo callback para detalle
) : RecyclerView.Adapter<SolicitudGestionTecnicoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_pendiente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        holder.txtNombre.text = item.nombreCliente
        holder.txtServicio.text = item.especialidadRequerida
        
        // Formato decimal con punto (Locale.US)
        holder.txtTarifa.text = String.format(Locale.US, "S/ %.2f", item.montoFinal)
        
        val sdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
        holder.txtFecha.text = item.fechaServicioProgramado?.let { sdf.format(it.toDate()) } ?: "No definida"

        if (item.fotoCliente.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.fotoCliente).circleCrop().into(holder.imgCliente)
        }

        // Al hacer clic en la tarjeta se muestra el detalle
        holder.itemView.setOnClickListener { onDetail(item) }

        // Reset visibilities
        holder.btnAceptar.visibility = View.GONE
        holder.btnRechazar.visibility = View.GONE
        holder.btnFinalizar.visibility = View.GONE
        holder.btnChat.visibility = View.GONE

        when (item.estado) {
            "pendiente" -> {
                holder.btnAceptar.visibility = View.VISIBLE
                holder.btnRechazar.visibility = View.VISIBLE
                holder.btnAceptar.setOnClickListener { onAceptar(item) }
                holder.btnRechazar.setOnClickListener { onRechazar(item) }
            }
            "aceptada", "en_curso" -> {
                holder.btnFinalizar.visibility = View.VISIBLE
                holder.btnChat.visibility = View.VISIBLE
                holder.btnFinalizar.setOnClickListener { onFinalizar(item) }
                holder.btnChat.setOnClickListener { onChat(item) }
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Solicitud>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgCliente: ImageView = view.findViewById(R.id.imgCliente)
        val txtNombre: TextView = view.findViewById(R.id.txtNombreCliente)
        val txtServicio: TextView = view.findViewById(R.id.txtServicio)
        val txtFecha: TextView = view.findViewById(R.id.txtFecha)
        val txtTarifa: TextView = view.findViewById(R.id.txtTarifa)
        val btnAceptar: Button = view.findViewById(R.id.btnAceptar)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazar)
        val btnFinalizar: Button = view.findViewById(R.id.btnFinalizar)
        val btnChat: Button = view.findViewById(R.id.btnChat)
    }
}
