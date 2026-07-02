package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Direccion

class DireccionAdapter(
    private var list: List<Direccion>,
    private val onEdit: (Direccion) -> Unit
) : RecyclerView.Adapter<DireccionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_direccion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvAlias.text = item.alias
        holder.tvDireccion.text = "${item.direccion}, ${item.distrito}"
        holder.tvBadge.visibility = if (item.esPrincipal) View.VISIBLE else View.GONE
        
        holder.btnEdit.setOnClickListener { onEdit(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Direccion>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAlias: TextView = view.findViewById(R.id.tvAlias)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvBadge: TextView = view.findViewById(R.id.tvBadgePrincipal)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditar)
    }
}
