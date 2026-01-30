package com.destajo.pintura

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class RegistrosAdapter(
    private val onClick: (Registro) -> Unit
) : RecyclerView.Adapter<RegistrosAdapter.ViewHolder>() {

    private var lista = listOf<Registro>()
    private val formatMoney = DecimalFormat("$#,##0.00")

    fun setRegistros(nuevos: List<Registro>) {
        lista = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.txtOrden.text = "#${item.orden}"
        holder.txtAuto.text = item.auto
        holder.txtDetalle.text = "${item.color} ${if(item.placa.isNotEmpty()) "• ${item.placa}" else ""}"

        // Icono de estado
        if(item.finalizado) {
            holder.iconEstado.setImageResource(android.R.drawable.checkbox_on_background)
            holder.iconEstado.setColorFilter(android.graphics.Color.parseColor("#16A34A")) // Verde
        } else {
            holder.iconEstado.setImageResource(android.R.drawable.checkbox_off_background)
            holder.iconEstado.setColorFilter(android.graphics.Color.parseColor("#CBD5E1")) // Gris
        }


        val sb = StringBuilder()
        var total = 0.0

        item.conceptos.forEach { c ->
            val monto = (c.monto.toDoubleOrNull() ?: 0.0) * ((c.porcentaje.toDoubleOrNull() ?: 0.0) / 100)
            total += monto
            // Agregamos línea: "• Pieza: $500.00"
            sb.append("• ${c.nombre}: ${formatMoney.format(monto)}\n")
        }


        if(item.observacion.isNotEmpty()) {
            sb.append("📝 ${item.observacion}")
        }

        holder.txtDesglose.text = sb.toString().trim()
        holder.txtTotal.text = formatMoney.format(total)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = lista.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val txtOrden: TextView = v.findViewById(R.id.txtOrden)
        val txtAuto: TextView = v.findViewById(R.id.txtAuto)
        val txtDetalle: TextView = v.findViewById(R.id.txtDetalleAuto)
        val txtDesglose: TextView = v.findViewById(R.id.txtDesgloseDinero)
        val txtTotal: TextView = v.findViewById(R.id.txtTotalTarjeta)
        val iconEstado: ImageView = v.findViewById(R.id.iconEstado)
    }
}