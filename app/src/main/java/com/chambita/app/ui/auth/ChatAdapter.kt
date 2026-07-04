package com.chambita.app.ui.auth

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Mensaje
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private var list: List<Mensaje>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        return if (list[position].remitenteId == currentUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = list[position]
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val hora = message.fechaRegistro?.let { sdf.format(it.toDate()) } ?: ""

        // Log para depuración
        Log.d("ChatAdapter", "Binding mensaje: ${message.texto} - Tipo: ${message.tipo}")

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
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Mensaje>) {
        list = newList
        notifyDataSetChanged()
    }

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val imgMensaje: ImageView = view.findViewById(R.id.imgMensaje)
        val tvHora: TextView = view.findViewById(R.id.tvHora)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val imgMensaje: ImageView = view.findViewById(R.id.imgMensaje)
        val tvHora: TextView = view.findViewById(R.id.tvHora)
    }
}
