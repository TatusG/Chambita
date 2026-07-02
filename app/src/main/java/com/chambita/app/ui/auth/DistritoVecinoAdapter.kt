package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R

data class DistritoVecino(val nombre: String, val cantidadTecnicos: Int)

class DistritoVecinoAdapter(
    private val lista: List<DistritoVecino>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<DistritoVecinoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Usando un layout genérico o el que corresponda si existe uno para el item
        // Dado que no se mencionó un item específico, usaré uno simple o inferiré de la imagen
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_distrito_vecino, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = item.nombre
        holder.tvCantidad.text = "${item.cantidadTecnicos} técnicos"
        holder.itemView.setOnClickListener { onClick(item.nombre) }
    }

    override fun getItemCount() = lista.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreDistrito)
        val tvCantidad: TextView = view.findViewById(R.id.tvCantidadTecnicos)
    }
}
