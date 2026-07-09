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
import com.chambita.app.models.Mensaje
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var list: List<Mensaje>,
    private val onPropuestaAction: (Mensaje, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT     = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val VIEW_TYPE_PROPOSAL = 3

    override fun getItemViewType(position: Int): Int {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val msg = list[position]
        return when {
            msg.tipo == "propuesta" || msg.tipo == "pago_final" -> VIEW_TYPE_PROPOSAL
            msg.remitenteId == currentUid -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT     -> SentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false))
            VIEW_TYPE_PROPOSAL -> ProposalViewHolder(inflater.inflate(R.layout.item_message_proposal, parent, false))
            else               -> ReceivedViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = list[position]
        val sdf  = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val hora = message.fechaRegistro?.let { sdf.format(it.toDate()) } ?: ""

        when (holder) {
            is SentViewHolder -> {
                holder.tvHora.text = hora
                if (message.tipo == "imagen") {
                    holder.tvMensaje.visibility = View.GONE
                    holder.imgMensaje.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context).load(message.texto).into(holder.imgMensaje)
                } else {
                    holder.tvMensaje.visibility = View.VISIBLE
                    holder.imgMensaje.visibility = View.GONE
                    holder.tvMensaje.text = message.texto
                }
            }

            is ReceivedViewHolder -> {
                holder.tvHora.text = hora
                if (message.tipo == "imagen") {
                    holder.tvMensaje.visibility = View.GONE
                    holder.imgMensaje.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context).load(message.texto).into(holder.imgMensaje)
                } else {
                    holder.tvMensaje.visibility = View.VISIBLE
                    holder.imgMensaje.visibility = View.GONE
                    holder.tvMensaje.text = message.texto
                }
            }

            is ProposalViewHolder -> {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                val esMia = message.remitenteId == currentUid

                holder.tvTitulo.text = if (message.tipo == "propuesta") "PROPUESTA DE PRECIO" else "CONFIRMACIÓN DE PAGO"
                holder.tvMonto.text  = "S/ ${message.monto.toInt()}"
                
                // Color del monto según diseño (Azul para resaltar cifra)
                holder.tvMonto.setTextColor(holder.itemView.context.getColor(R.color.figma_blue))
                
                // --- LÓGICA DE ESTADOS ---
                if (message.estadoPropuesta == "aceptada") {
                    holder.layoutAcciones.visibility = View.GONE
                    holder.tvDetalle.text = if (esMia) "✓ Propuesta aceptada por el cliente" else "✓ Has aceptado esta propuesta"
                    holder.tvDetalle.setTextColor(holder.itemView.context.getColor(R.color.chambita_verde))
                } else if (message.estadoPropuesta == "rechazada") {
                    holder.layoutAcciones.visibility = View.GONE
                    holder.tvDetalle.text = if (esMia) "✗ Propuesta rechazada por el cliente" else "✗ Has rechazado esta propuesta"
                    holder.tvDetalle.setTextColor(holder.itemView.context.getColor(R.color.chambita_rojo))
                } else {
                    // PENDIENTE
                    holder.tvDetalle.setTextColor(holder.itemView.context.getColor(R.color.chambita_texto_secundario))
                    if (!esMia) {
                        holder.layoutAcciones.visibility = View.VISIBLE
                        holder.tvDetalle.text = "Propuesta por el servicio"
                        
                        holder.btnAceptar.setOnClickListener { onPropuestaAction(message, "ACEPTAR") }
                        holder.btnRechazar.setOnClickListener { onPropuestaAction(message, "RECHAZAR") }
                    } else {
                        holder.layoutAcciones.visibility = View.GONE
                        holder.tvDetalle.text = "Esperando confirmación del cliente..."
                    }
                }
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Mensaje>) {
        list = newList
        notifyDataSetChanged()
    }

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView  = view.findViewById(R.id.tvMensaje)
        val imgMensaje: ImageView = view.findViewById(R.id.imgMensaje)
        val tvHora: TextView     = view.findViewById(R.id.tvHora)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView  = view.findViewById(R.id.tvMensaje)
        val imgMensaje: ImageView = view.findViewById(R.id.imgMensaje)
        val tvHora: TextView     = view.findViewById(R.id.tvHora)
    }

    class ProposalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView     = view.findViewById(R.id.tvPropuestaTitulo)
        val tvMonto: TextView      = view.findViewById(R.id.tvPropuestaMonto)
        val tvDetalle: TextView    = view.findViewById(R.id.tvPropuestaDetalle)
        val layoutAcciones: View   = view.findViewById(R.id.layoutAccionesPropuesta)
        val btnAceptar: Button     = view.findViewById(R.id.btnAceptarPropuesta)
        val btnRechazar: Button    = view.findViewById(R.id.btnRechazarPropuesta)
    }
}
