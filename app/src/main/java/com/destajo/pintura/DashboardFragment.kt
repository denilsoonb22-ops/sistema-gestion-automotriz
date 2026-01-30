package com.destajo.pintura

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val db = FirebaseFirestore.getInstance()
    private val formatMoney = DecimalFormat("$#,##0.00")

    private var totalGanado = 0.0
    private var costoMaterial = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lblGanado = view.findViewById<TextView>(R.id.lblGanado)
        val lblAutos = view.findViewById<TextView>(R.id.lblAutos)
        val lblNeta = view.findViewById<TextView>(R.id.lblNeta)
        val lblMaterial = view.findViewById<TextView>(R.id.lblMaterial)

        val cardAviso = view.findViewById<View>(R.id.cardAvisoJueves)
        val btnEditarMat = view.findViewById<View>(R.id.btnEditarMaterial)

        // Mostrar fecha actual
        val sdf = SimpleDateFormat("dd MMMM", Locale.getDefault())



        btnEditarMat.setOnClickListener {
            mostrarDialogoMaterial()
        }


        val (inicio, fin) = getRangoSemanaActual()
        val weekKey = "mat_" + android.text.format.DateFormat.format("yyyy-MM-dd", inicio).toString().replace("-", "")


        db.collection("configuracion").document("gastos_semanales").addSnapshotListener { s, _ ->
            if (s != null && s.exists()) {
                costoMaterial = s.getDouble(weekKey) ?: 0.0
                // Solo mostramos el texto, no se edita aquí
                lblMaterial.text = formatMoney.format(costoMaterial)
                actualizarNeto(lblNeta)
            }
        }


        db.collection("registros")
            .whereGreaterThanOrEqualTo("fecha", android.text.format.DateFormat.format("yyyy-MM-dd", inicio).toString())
            .addSnapshotListener { s, _ ->
                if (s == null) return@addSnapshotListener
                var suma = 0.0
                var terminados = 0
                val total = s.size()

                for (doc in s) {
                    val r = doc.toObject(Registro::class.java)
                    if (r.finalizado) terminados++

                    r.conceptos.forEach { c ->
                        val m = c.monto.toDoubleOrNull() ?: 0.0
                        val p = c.porcentaje.toDoubleOrNull() ?: 0.0
                        suma += m * (p/100)
                    }
                }

                totalGanado = suma
                lblGanado.text = formatMoney.format(totalGanado)
                lblAutos.text = "$terminados / $total"
                actualizarNeto(lblNeta)

                // Aviso Jueves
                val hoy = Calendar.getInstance()
                if (hoy.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY && (total - terminados) > 0) {
                    cardAviso.visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.txtAvisoJueves).text = "¡Es Jueves! Tienes ${total - terminados} pendientes."
                } else {
                    cardAviso.visibility = View.GONE
                }
            }
    }

    private fun mostrarDialogoMaterial() {
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Ej. 1500.50"
        input.setText(if (costoMaterial > 0) costoMaterial.toString() else "")
        input.textAlignment = View.TEXT_ALIGNMENT_CENTER
        input.textSize = 24f

        // Contenedor para darle margen
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(context)
            .setTitle("Gasto de Material Semanal")

            .setView(container)
            .setPositiveButton("GUARDAR") { _, _ ->
                val valor = input.text.toString().toDoubleOrNull() ?: 0.0
                guardarMaterialFirebase(valor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarMaterialFirebase(valor: Double) {
        val (inicio, _) = getRangoSemanaActual()
        val weekKey = "mat_" + android.text.format.DateFormat.format("yyyy-MM-dd", inicio).toString().replace("-", "")

        db.collection("configuracion").document("gastos_semanales")
            .update(weekKey, valor)
            .addOnFailureListener {
                val map = hashMapOf(weekKey to valor)
                db.collection("configuracion").document("gastos_semanales").set(map, com.google.firebase.firestore.SetOptions.merge())
            }
        Toast.makeText(context, "Gasto actualizado", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarNeto(lbl: TextView) {
        val neto = totalGanado - costoMaterial
        lbl.text = formatMoney.format(neto)
    }

    private fun getRangoSemanaActual(): Pair<Date, Date> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)

        val dia = cal.get(Calendar.DAY_OF_WEEK)
        val resta = when(dia) {
            Calendar.FRIDAY -> 0
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY -> 2
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -resta)
        val inicio = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val fin = cal.time
        return Pair(inicio, fin)
    }
}