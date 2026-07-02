package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Solicitud
import com.google.firebase.firestore.FirebaseFirestore

class SolicitudAdapter(
    private var lista: List<Solicitud>,
    private val onAction: (Solicitud, String) -> Unit
) : RecyclerView.Adapter<SolicitudAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtEspecialidad: TextView = view.findViewById(R.id.txtEspecialidad)
        val txtEstado: TextView = view.findViewById(R.id.txtEstado)
        val txtDescripcion: TextView = view.findViewById(R.id.txtDescripcion)
        val layoutTecnico: LinearLayout = view.findViewById(R.id.layoutTecnicoAsignado)
        val txtNombreTecnico: TextView = view.findViewById(R.id.txtNombreTecnico)
        val txtMensajeEspera: TextView = view.findViewById(R.id.txtMensajeEspera)
        val btnChat: ImageButton = view.findViewById(R.id.btnChatTecnico)
        val btnCalificar: Button = view.findViewById(R.id.btnCalificarServicio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val solicitud = lista[position]

        holder.txtEspecialidad.text = solicitud.especialidadRequerida.uppercase()
        holder.txtEstado.text = solicitud.estado.uppercase()
        holder.txtDescripcion.text = solicitud.descripcionAveria

        if (solicitud.estado == "finalizada" && !solicitud.resenaDejada) {
            holder.btnCalificar.visibility = View.VISIBLE
            holder.btnCalificar.setOnClickListener { onAction(solicitud, "CALIFICAR") }
        } else {
            holder.btnCalificar.visibility = View.GONE
        }

        if (solicitud.tecnicoId != null) {
            holder.layoutTecnico.visibility = View.VISIBLE
            holder.txtMensajeEspera.visibility = View.GONE
            
            FirebaseFirestore.getInstance().collection("usuarios").document(solicitud.tecnicoId)
                .get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        holder.txtNombreTecnico.text = doc.getString("nombreCompleto")
                    }
                }

            holder.btnChat.setOnClickListener { onAction(solicitud, "CHAT") }
        } else {
            holder.layoutTecnico.visibility = View.GONE
            holder.txtMensajeEspera.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = lista.size

    fun updateList(nuevaLista: List<Solicitud>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
