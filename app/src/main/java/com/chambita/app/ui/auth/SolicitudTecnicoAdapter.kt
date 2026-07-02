package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Solicitud

class SolicitudTecnicoAdapter(
    private var list: List<Solicitud>,
    private val onAceptar: (Solicitud) -> Unit,
    private val onRechazar: (Solicitud) -> Unit
) : RecyclerView.Adapter<SolicitudTecnicoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_home_tecnico, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtCliente.text = item.nombreCliente
        holder.txtTrabajo.text = "${item.especialidadRequerida} · ${item.direccionServicio.split(",").lastOrNull() ?: ""}"
        
        holder.btnAceptar.setOnClickListener { onAceptar(item) }
        holder.btnRechazar.setOnClickListener { onRechazar(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Solicitud>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCliente: TextView = view.findViewById(R.id.txtCliente)
        val txtTrabajo: TextView = view.findViewById(R.id.txtTrabajo)
        val btnAceptar: Button = view.findViewById(R.id.btnAceptar)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazar)
    }
}
