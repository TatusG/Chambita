package com.chambita.app.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
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
        holder.txtAlias.text = item.alias
        
        // Uso de recurso de cadena con marcadores de posición para buenas prácticas
        holder.txtDireccion.text = holder.itemView.context.getString(
            R.string.address_format,
            item.direccion,
            item.distrito
        )
        
        holder.txtPrincipal.visibility = if (item.esPrincipal) View.VISIBLE else View.GONE
        
        // Cambiar ícono según el alias
        val iconRes = when (item.alias.lowercase()) {
            "casa" -> R.drawable.ic_home
            "trabajo" -> R.drawable.ic_pin
            else -> R.drawable.ic_location
        }
        holder.imgIcono.setImageResource(iconRes)

        holder.btnEditar.setOnClickListener { onEdit(item) }
    }

    override fun getItemCount() = list.size

    // Uso de DiffUtil para actualizaciones eficientes (Buenas prácticas)
    fun updateList(newList: List<Direccion>) {
        val diffCallback = DireccionDiffCallback(this.list, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtAlias: TextView = view.findViewById(R.id.txtAlias)
        val txtDireccion: TextView = view.findViewById(R.id.txtDireccion)
        val txtPrincipal: TextView = view.findViewById(R.id.txtPrincipal)
        val btnEditar: TextView = view.findViewById(R.id.btnEditar)
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
    }

    class DireccionDiffCallback(
        private val oldList: List<Direccion>,
        private val newList: List<Direccion>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].id == newList[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}
