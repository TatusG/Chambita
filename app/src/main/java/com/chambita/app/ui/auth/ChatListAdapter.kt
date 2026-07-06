package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Chat
import com.chambita.app.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private var list: List<Chat>,
    private val onClick: (Chat, Usuario?) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = list[position]
        val contactoId = if (chat.clienteId == currentUid) chat.tecnicoId else chat.clienteId
        
        if (contactoId.isEmpty()) return

        holder.txtMensaje.text = chat.ultimoMensaje
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.txtHora.text = chat.fechaUltimoMensaje?.let { sdf.format(it.toDate()) } ?: ""

        // Cargar datos del contacto
        FirebaseFirestore.getInstance().collection("usuarios").document(contactoId)
            .get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val contacto = doc.toObject(Usuario::class.java)
                    contacto?.let {
                        holder.txtNombre.text = it.nombreCompleto
                        if (it.fotoPerfil.isNotEmpty()) {
                            Glide.with(holder.itemView.context).load(it.fotoPerfil).circleCrop().into(holder.imgAvatar)
                        } else {
                            holder.imgAvatar.setImageResource(R.drawable.ic_person)
                        }
                        holder.itemView.setOnClickListener { onClick(chat, contacto) }
                    }
                }
            }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Chat>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val txtNombre: TextView = view.findViewById(R.id.txtNombre)
        val txtMensaje: TextView = view.findViewById(R.id.txtMensaje)
        val txtHora: TextView = view.findViewById(R.id.txtHora)
    }
}
