package com.destajo.pintura

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AgregarFragment : Fragment(R.layout.fragment_agregar) {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var contenedorConceptos: LinearLayout
    private val opcionesConcepto = listOf("Aseguradora", "Reacondicionado", "Rin", "Particular", "Otro")
    private var registroEditar: Registro? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contenedorConceptos = view.findViewById(R.id.contenedorConceptos)
        val btnAdd = view.findViewById<Button>(R.id.btnAgregarConcepto)
        val btnSave = view.findViewById<Button>(R.id.btnGuardar)

        // Recibir datos si es Edición
        arguments?.let { registroEditar = it.getSerializable("registro_editar") as? Registro }

        if (registroEditar != null) {
            btnSave.text = "ACTUALIZAR DATOS"
            cargarDatos(view, registroEditar!!)
        } else {
            agregarFilaConcepto()
        }

        btnAdd.setOnClickListener { agregarFilaConcepto() }
        btnSave.setOnClickListener { guardar() }
    }

    private fun cargarDatos(view: View, r: Registro) {
        view.findViewById<TextInputEditText>(R.id.editAuto).setText(r.auto)
        view.findViewById<TextInputEditText>(R.id.editColor).setText(r.color)
        view.findViewById<TextInputEditText>(R.id.editOrden).setText(r.orden)
        view.findViewById<TextInputEditText>(R.id.editPlaca).setText(r.placa)
        view.findViewById<TextInputEditText>(R.id.editObs).setText(r.observacion)
        r.conceptos.forEach { agregarFilaConcepto(it.nombre, it.monto, it.porcentaje) }
    }

    private fun agregarFilaConcepto(nombre: String? = null, monto: String? = null, porcentaje: String? = null) {
        val fila = LayoutInflater.from(context).inflate(R.layout.item_concepto_input, contenedorConceptos, false)
        val spin = fila.findViewById<Spinner>(R.id.spinnerConcepto)
        spin.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opcionesConcepto)

        if (nombre != null) {
            spin.setSelection((spin.adapter as ArrayAdapter<String>).getPosition(nombre))
            fila.findViewById<EditText>(R.id.inputMonto).setText(monto)
            fila.findViewById<EditText>(R.id.inputPorcentaje).setText(porcentaje)
        }

        fila.findViewById<ImageButton>(R.id.btnEliminar).setOnClickListener { contenedorConceptos.removeView(fila) }
        contenedorConceptos.addView(fila)
    }

    private fun guardar() {
        val view = view ?: return
        val auto = view.findViewById<TextInputEditText>(R.id.editAuto).text.toString().uppercase().trim()
        val orden = view.findViewById<TextInputEditText>(R.id.editOrden).text.toString().trim()

        if (auto.isEmpty() || orden.isEmpty()) {
            Toast.makeText(context, "Falta Auto u Orden", Toast.LENGTH_SHORT).show()
            return
        }

        val conceptos = mutableListOf<Map<String, String>>()
        for (i in 0 until contenedorConceptos.childCount) {
            val f = contenedorConceptos.getChildAt(i)
            val nom = f.findViewById<Spinner>(R.id.spinnerConcepto).selectedItem.toString()
            val mon = f.findViewById<EditText>(R.id.inputMonto).text.toString()
            val por = f.findViewById<EditText>(R.id.inputPorcentaje).text.toString()
            if (mon.isNotEmpty()) conceptos.add(mapOf("nombre" to nom, "monto" to mon, "porcentaje" to por))
        }

        val data = hashMapOf(
            "auto" to auto,
            "color" to view.findViewById<TextInputEditText>(R.id.editColor).text.toString().uppercase().trim(),
            "orden" to orden,
            "placa" to view.findViewById<TextInputEditText>(R.id.editPlaca).text.toString().uppercase().trim(),
            "observacion" to view.findViewById<TextInputEditText>(R.id.editObs).text.toString().trim(),
            "conceptos" to conceptos,
            "fecha" to (registroEditar?.fecha ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())),
            "finalizado" to (registroEditar?.finalizado ?: false)
        )
        if (registroEditar == null) data["createdAt"] = com.google.firebase.Timestamp.now()

        val btn = view.findViewById<Button>(R.id.btnGuardar)
        btn.isEnabled = false; btn.text = "GUARDANDO..."

        val ref = if (registroEditar != null) db.collection("registros").document(registroEditar!!.id) else db.collection("registros").document()

        ref.set(data) // .set funciona para crear o sobrescribir si pasamos el ID
            .addOnSuccessListener {
                Toast.makeText(context, "Guardado Correctamente", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                btn.isEnabled = true; btn.text = "INTENTAR DE NUEVO"
                Toast.makeText(context, "Error al guardar (se subirá luego)", Toast.LENGTH_SHORT).show()
            }
    }
}