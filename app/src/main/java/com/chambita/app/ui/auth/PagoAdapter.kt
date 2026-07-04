package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chambita.app.R
import com.chambita.app.models.Pago
import java.text.SimpleDateFormat
import java.util.*

class PagoAdapter(private var list: List<Pago>) : RecyclerView.Adapter<PagoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pago, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtMonto.text = "S/ %.2f".format(item.monto)
        holder.txtMetodo.text = item.metodoUsado
        
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        holder.txtFecha.text = item.fechaRegistro?.let { sdf.format(it.toDate()) } ?: ""
        
        val icon = when(item.metodoUsado) {
            "Yape" -> R.drawable.ic_yape
            "Plin" -> R.drawable.ic_plin
            else -> R.drawable.ic_payment
        }
        holder.imgIcon.setImageResource(icon)
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Pago>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgIcon: ImageView = v.findViewById(R.id.imgPagoIcon)
        val txtMetodo: TextView = v.findViewById(R.id.txtPagoMetodo)
        val txtFecha: TextView = v.findViewById(R.id.txtPagoFecha)
        val txtMonto: TextView = v.findViewById(R.id.txtPagoMonto)
    }
}
