package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.MetodoPago

class MetodoPagoAdapter(
    private var list: List<MetodoPago>,
    private val onSetDefault: (MetodoPago) -> Unit
) : RecyclerView.Adapter<MetodoPagoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_metodo_pago, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtTipo.text = item.tipo
        holder.txtNumero.text = item.numeroAsociado
        
        val icon = when(item.tipo) {
            "Yape" -> R.drawable.ic_yape
            "Plin" -> R.drawable.ic_plin
            else -> R.drawable.ic_payment
        }
        holder.imgIcon.setImageResource(icon)
        
        holder.txtDefault.visibility = if (item.esPredeterminado) View.VISIBLE else View.GONE
        
        holder.itemView.setOnClickListener { onSetDefault(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<MetodoPago>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgIcon: ImageView = v.findViewById(R.id.imgMetodoIcon)
        val txtTipo: TextView = v.findViewById(R.id.txtMetodoTipo)
        val txtNumero: TextView = v.findViewById(R.id.txtMetodoNumero)
        val txtDefault: TextView = v.findViewById(R.id.txtMetodoDefault)
    }
}
