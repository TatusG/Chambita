package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = list[position]
        val contactoId = if (chat.clienteId == currentUid) chat.tecnicoId else chat.clienteId

        if (contactoId.isEmpty()) return

        // ✅ Texto por defecto mientras carga Firestore
        holder.txtNombre.text  = "Cargando..."
        holder.txtMensaje.text = chat.ultimoMensaje

        val sdf = SimpleDateFormat("HH:mm a", Locale.US)
        holder.txtHora.text = chat.fechaUltimoMensaje?.let { sdf.format(it.toDate()) } ?: ""

        // ✅ Click inmediato — NO depende de que Firestore haya respondido
        // Usa contactoId directamente desde el objeto Chat
        holder.itemView.setOnClickListener {
            onClick(chat, null) // contacto puede ser null — MensajesActivity ya lo maneja
        }

        // Cargar datos del contacto para mostrar nombre, foto y estado
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(contactoId)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !doc.exists()) {
                    // ✅ Aunque falle, el click ya funciona con contactoId del Chat
                    holder.txtNombre.text = "Usuario"
                    return@addSnapshotListener
                }

                val contacto = doc.toObject(Usuario::class.java) ?: return@addSnapshotListener

                holder.txtNombre.text = contacto.nombreCompleto.ifEmpty { "Usuario" }

                // Estado online
                val color = if (contacto.estaEnLinea) R.color.chambita_verde else R.color.chambita_rojo
                holder.viewOnline.setBackgroundResource(R.drawable.bg_online_dot)
                holder.viewOnline.backgroundTintList =
                    ContextCompat.getColorStateList(holder.itemView.context, color)

                // Foto de perfil
                if (contacto.fotoPerfil.isNotEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(contacto.fotoPerfil)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(holder.imgAvatar)
                } else {
                    holder.imgAvatar.setImageResource(R.drawable.ic_person)
                }

                // ✅ Actualizar click con el contacto completo ya cargado
                holder.itemView.setOnClickListener {
                    onClick(chat, contacto)
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
        val txtNombre: TextView  = view.findViewById(R.id.txtNombre)
        val txtMensaje: TextView = view.findViewById(R.id.txtMensaje)
        val txtHora: TextView    = view.findViewById(R.id.tvHora)
        val viewOnline: View     = view.findViewById(R.id.viewOnline)
    }
}