package com.destajo.pintura

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class ListaFragment : Fragment(R.layout.fragment_lista) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: RegistrosAdapter
    private var firestoreListener: ListenerRegistration? = null

    // Variables de Filtro
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var usandoRango = false

    // Datos
    private var listaActual: List<Registro> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRegistros)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val txtVacio = view.findViewById<TextView>(R.id.txtVacio)

        view.findViewById<View>(R.id.fabHerramientas).setOnClickListener {
            mostrarPanelHerramientas()
        }

        adapter = RegistrosAdapter { registro -> mostrarMenuOpciones(registro) }
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        // Iniciar con la semana actual
        calcularSemanaActual()
        cargarDatos(progressBar, txtVacio)
    }

    private fun calcularSemanaActual() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)

        val diaSemana = cal.get(Calendar.DAY_OF_WEEK)
        val diasARestar = when (diaSemana) {
            Calendar.FRIDAY -> 0; Calendar.SATURDAY -> 1; Calendar.SUNDAY -> 2; Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4; Calendar.WEDNESDAY -> 5; Calendar.THURSDAY -> 6; else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -diasARestar)
        fechaInicio = cal.time
        fechaFin = null
        usandoRango = false
    }

    private fun cargarDatos(progressBar: ProgressBar, txtVacio: TextView) {
        if (auth.currentUser == null) return
        progressBar.visibility = View.VISIBLE

        var query: Query = db.collection("registros").orderBy("fecha", Query.Direction.DESCENDING)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        if (usandoRango && fechaInicio != null && fechaFin != null) {
            query = query.whereGreaterThanOrEqualTo("fecha", sdf.format(fechaInicio!!))
                .whereLessThanOrEqualTo("fecha", sdf.format(fechaFin!!))
        } else if (fechaInicio != null) {
            query = query.whereGreaterThanOrEqualTo("fecha", sdf.format(fechaInicio!!))
        }

        firestoreListener?.remove()
        firestoreListener = query.addSnapshotListener { snapshots, error ->
            if (!isAdded) return@addSnapshotListener
            progressBar.visibility = View.GONE
            if (error != null) return@addSnapshotListener

            if (snapshots != null) {
                val lista = snapshots.toObjects(Registro::class.java)
                lista.forEachIndexed { i, reg -> reg.id = snapshots.documents[i].id }
                listaActual = lista
                adapter.setRegistros(lista)
                txtVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun mostrarPanelHerramientas() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_filtros, null)
        dialog.setContentView(view)

        var tempInicio: Date? = if(usandoRango) fechaInicio else null
        var tempFin: Date? = if(usandoRango) fechaFin else null
        val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val btnDesde = view.findViewById<Button>(R.id.btnFechaDesde)
        val btnHasta = view.findViewById<Button>(R.id.btnFechaHasta)
        val inputBusqueda = view.findViewById<TextView>(R.id.inputBusqueda)

        // BUSCADOR
        view.findViewById<Button>(R.id.btnBuscarTexto).setOnClickListener {
            val txt = inputBusqueda.text.toString().uppercase().trim()
            if (txt.isNotEmpty()) {
                dialog.dismiss()
                buscarPorTexto(txt)
            }
        }

        // FECHAS
        btnDesde.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val c = Calendar.getInstance(); c.set(y,m,d)
                tempInicio = c.time
                btnDesde.text = sdfDisplay.format(tempInicio!!)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnHasta.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val c = Calendar.getInstance(); c.set(y,m,d)
                tempFin = c.time
                btnHasta.text = sdfDisplay.format(tempFin!!)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        view.findViewById<Button>(R.id.btnAplicarRango).setOnClickListener {
            if(tempInicio != null && tempFin != null) {
                fechaInicio = tempInicio; fechaFin = tempFin; usandoRango = true
                cargarDatos(requireView().findViewById(R.id.progressBar), requireView().findViewById(R.id.txtVacio))
                dialog.dismiss()
            } else { Toast.makeText(context, "Selecciona ambas fechas", Toast.LENGTH_SHORT).show() }
        }

        view.findViewById<Button>(R.id.btnResetSemana).setOnClickListener {
            calcularSemanaActual()
            cargarDatos(requireView().findViewById(R.id.progressBar), requireView().findViewById(R.id.txtVacio))
            dialog.dismiss()
        }

        // --- BOTONES REPORTES ---
        view.findViewById<View>(R.id.btnPdfSimple).setOnClickListener {
            generarPDFDinamic(conDinero = false, gastoMaterial = 0.0)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnPdfTotal).setOnClickListener {
            dialog.dismiss()
            obtenerMaterialYGenerar()
        }
        view.findViewById<View>(R.id.btnCSV).setOnClickListener {
            dialog.dismiss()
            generarExcelCSV()
        }

        dialog.show()
    }

    // --- LECTURA DE MATERIAL (MODO FIJO ACTUAL) ---
    private fun obtenerMaterialYGenerar() {
        val pb = requireView().findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility = View.VISIBLE

        // CAMBIO: Usamos SIEMPRE la fecha de HOY para buscar el material
        val cal = Calendar.getInstance() // Fecha Actual

        // Calculamos el viernes de la semana ACTUAL
        val diaSemana = cal.get(Calendar.DAY_OF_WEEK)
        val diasARestar = when (diaSemana) {
            Calendar.FRIDAY -> 0; Calendar.SATURDAY -> 1; Calendar.SUNDAY -> 2; Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4; Calendar.WEDNESDAY -> 5; Calendar.THURSDAY -> 6; else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -diasARestar)
        val viernesDeCorte = cal.time

        // Generar llave: mat_YYYYMMDD
        val sdfKey = SimpleDateFormat("yyyyMMdd", Locale.US)
        val weekKey = "mat_" + sdfKey.format(viernesDeCorte)

        db.collection("configuracion").document("gastos_semanales").get()
            .addOnSuccessListener { document ->
                pb.visibility = View.GONE
                // Obtener valor seguro
                val valorRaw = document.get(weekKey)
                val materialGuardado = when (valorRaw) {
                    is Double -> valorRaw
                    is Long -> valorRaw.toDouble()
                    is String -> valorRaw.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }

                generarPDFDinamic(conDinero = true, gastoMaterial = materialGuardado)
            }
            .addOnFailureListener {
                pb.visibility = View.GONE
                Toast.makeText(context, "Error conectando con Firebase", Toast.LENGTH_SHORT).show()
                generarPDFDinamic(conDinero = true, gastoMaterial = 0.0)
            }
    }

    private fun buscarPorTexto(texto: String) {
        val pb = requireView().findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility = View.VISIBLE
        db.collection("registros").orderBy("fecha", Query.Direction.DESCENDING).limit(100).get()
            .addOnSuccessListener { snaps ->
                pb.visibility = View.GONE
                val todos = snaps.toObjects(Registro::class.java)
                todos.forEachIndexed { i, r -> r.id = snaps.documents[i].id }

                val filtrados = todos.filter {
                    (it.auto ?: "").contains(texto) || (it.placa ?: "").contains(texto) || (it.orden ?: "").contains(texto)
                }
                listaActual = filtrados
                adapter.setRegistros(filtrados)
                if(filtrados.isEmpty()) Toast.makeText(context, "No encontrado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarMenuOpciones(registro: Registro) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_opciones_registro, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.txtDialogTitulo).text = "${registro.auto ?: "Auto"} (${registro.orden ?: "-"})"
        view.findViewById<TextView>(R.id.txtOpcEstado).text = if (registro.finalizado) "Marcar PENDIENTE" else "Marcar FINALIZADO"

        view.findViewById<View>(R.id.opcEditar).setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle()
            bundle.putSerializable("registro_editar", registro)
            findNavController().navigate(R.id.nav_agregar, bundle)
        }
        view.findViewById<View>(R.id.opcEstado).setOnClickListener {
            dialog.dismiss()
            db.collection("registros").document(registro.id).update("finalizado", !registro.finalizado)
                .addOnSuccessListener { Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT).show() }
        }
        view.findViewById<View>(R.id.opcBorrar).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(context).setTitle("Eliminar").setMessage("¿Borrar definitivamente?")
                .setPositiveButton("SI") { _,_ -> db.collection("registros").document(registro.id).delete() }
                .setNegativeButton("NO", null).show()
        }
        dialog.show()
    }

    // =========================================================================================
    //   GENERADOR PDF DINÁMICO (DISEÑO FINAL ARREGLADO)
    // =========================================================================================
    private fun generarPDFDinamic(conDinero: Boolean, gastoMaterial: Double) {
        if (listaActual.isEmpty()) {
            Toast.makeText(context, "⚠️ No hay datos para el reporte.", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreArchivo = if (conDinero) "Reporte_Finanzas.pdf" else "Ordenes_Taller.pdf"
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), nombreArchivo)
        val doc = PdfDocument()

        try {
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas

            // --- ESTILOS ---
            val textPaint = TextPaint().apply { isAntiAlias = true; textSize = 9f; color = Color.BLACK }
            val textPaintBold = TextPaint().apply { isAntiAlias = true; textSize = 9f; color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD }
            val textPaintHeader = TextPaint().apply { isAntiAlias = true; textSize = 10f; color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD }
            // CORRECCION: Quitamos el Align.RIGHT del Paint base para que el StaticLayout funcione bien
            val textPaintMonto = TextPaint().apply { isAntiAlias = true; textSize = 9f; color = Color.parseColor("#16A34A"); typeface = Typeface.DEFAULT_BOLD }

            val paintLineas = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f }
            val paintFondoHeader = Paint().apply { color = Color.parseColor("#E2E8F0") }

            val margen = 30f
            val anchoPagina = 595f - (2 * margen) // ~535px

            // 1. TÍTULO Y PERIODO
            val titulo = if (conDinero) "REPORTE FINANCIERO" else "LISTADO DE ORDENES"
            val paintTitulo = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; color = Color.parseColor("#0F172A") }
            canvas.drawText(titulo, margen, 50f, paintTitulo)

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val txtInicio = if(fechaInicio != null) sdf.format(fechaInicio!!) else "..."
            val txtFin = if(fechaFin != null) sdf.format(fechaFin!!) else "..."
            canvas.drawText("Periodo: $txtInicio al $txtFin", margen, 70f, TextPaint().apply { textSize=11f; color=Color.DKGRAY })

            // 2. DEFINICIÓN DE COLUMNAS (CORRECCIÓN DE ANCHOS)
            var y = 100f

            val wCol1: Float // Orden
            val wCol2: Float // Auto
            val wCol3: Float // Conceptos
            val wCol4: Float // Notas
            val wCol5: Float // Total

            val GAP = 10f

            if (conDinero) {
                // DISEÑO FINANCIERO: Total empujado a la derecha
                wCol1 = 35f  // Orden
                wCol2 = 75f  // Auto
                wCol3 = 160f // Conceptos
                wCol5 = 75f  // Total

                // NOTAS toma el espacio restante
                val ocupado = wCol1 + wCol2 + wCol3 + wCol5 + (4 * GAP)
                wCol4 = anchoPagina - ocupado // Espacio automático para notas
            } else {
                // Taller
                wCol1 = 50f; wCol2 = 100f; wCol3 = 100f; wCol4 = 90f; wCol5 = 180f
            }

            // Coordenadas X
            val xCol1 = margen
            val xCol2 = xCol1 + wCol1 + GAP
            val xCol3 = xCol2 + wCol2 + GAP
            val xCol4 = xCol3 + wCol3 + GAP
            val xCol5 = xCol4 + wCol4 + GAP // Aquí empieza el Total

            // 3. DIBUJAR ENCABEZADOS
            canvas.drawRect(margen, y, margen + anchoPagina, y + 25f, paintFondoHeader)
            val yHead = y + 16f
            canvas.drawText("ORDEN", xCol1 + 2, yHead, textPaintHeader)

            if(conDinero) {
                canvas.drawText("VEHÍCULO", xCol2 + 2, yHead, textPaintHeader)
                canvas.drawText("CONCEPTOS", xCol3 + 2, yHead, textPaintHeader)
                canvas.drawText("NOTAS", xCol4 + 2, yHead, textPaintHeader)
                // TOTAL ALINEADO A LA DERECHA DE SU COLUMNA
                canvas.drawText("TOTAL", xCol5 + wCol5 - 2, yHead, textPaintHeader.apply { textAlign = Paint.Align.RIGHT })
                textPaintHeader.textAlign = Paint.Align.LEFT
            } else {
                canvas.drawText("AUTO", xCol2 + 2, yHead, textPaintHeader)
                canvas.drawText("COLOR", xCol3 + 2, yHead, textPaintHeader)
                canvas.drawText("PLACA", xCol4 + 2, yHead, textPaintHeader)
                canvas.drawText("NOTAS", xCol5 + 2, yHead, textPaintHeader)
            }
            y += 25f

            // 4. DIBUJAR FILAS
            var granTotal = 0.0

            for (item in listaActual) {
                val txtOrden = item.orden ?: "-"
                val txtCol2 = if(conDinero) "${item.auto}\n${item.color}" else (item.auto ?: "")
                val txtCol3: String
                var totalDineroFila = 0.0

                if (conDinero) {
                    val sb = StringBuilder()
                    (item.conceptos ?: emptyList()).forEach { c ->
                        val nombre = c.nombre ?: "Servicio"
                        val m = c.monto?.toDoubleOrNull() ?: 0.0
                        val p = c.porcentaje?.toDoubleOrNull() ?: 0.0
                        val g = m * (p/100)
                        totalDineroFila += g
                        sb.append("• $nombre ($${String.format("%.0f", m)} - ${String.format("%.0f", p)}%): $${String.format("%.2f", g)}\n")
                    }
                    txtCol3 = sb.toString().trim()
                } else {
                    txtCol3 = item.color ?: ""
                }
                granTotal += totalDineroFila

                val txtCol4 = if(conDinero) (item.observacion ?: "") else (item.placa ?: "")
                val txtCol5 = if(conDinero) "$${String.format("%.2f", totalDineroFila)}" else (item.observacion ?: "")

                val h1 = measureTextHeight(txtOrden, wCol1, textPaint)
                val h2 = measureTextHeight(txtCol2, wCol2, if(conDinero) textPaintBold else textPaint)
                val h3 = measureTextHeight(txtCol3, wCol3, textPaint)
                val h4 = measureTextHeight(txtCol4, wCol4, textPaint)
                val h5 = measureTextHeight(txtCol5, wCol5, if(conDinero) textPaintMonto else textPaint)

                val maxH = max(h1, max(h2, max(h3, max(h4, h5))))
                val rowHeight = maxH + 15f

                if (y + rowHeight > 800f) {
                    doc.finishPage(page)
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, doc.pages.size + 1).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                    canvas.drawRect(margen, y, margen + anchoPagina, y + 25f, paintFondoHeader)
                    canvas.drawText("ORDEN", xCol1 + 2, y + 16f, textPaintHeader)
                    y += 25f
                }

                val yText = y + 5f
                drawMultiline(canvas, txtOrden, xCol1 + 2, yText, wCol1, textPaint)
                drawMultiline(canvas, txtCol2, xCol2 + 2, yText, wCol2, if(conDinero) textPaintBold else textPaint)
                drawMultiline(canvas, txtCol3, xCol3 + 2, yText, wCol3, textPaint)
                drawMultiline(canvas, txtCol4, xCol4 + 2, yText, wCol4, textPaint)

                if (conDinero) {
                    // Total Derecha (Usamos ALIGN_OPPOSITE para forzarlo a la derecha)
                    drawMultiline(canvas, txtCol5, xCol5, yText, wCol5, textPaintMonto, Layout.Alignment.ALIGN_OPPOSITE)
                } else {
                    drawMultiline(canvas, txtCol5, xCol5 + 2, yText, wCol5, textPaint)
                }

                canvas.drawLine(margen, y + rowHeight, margen + anchoPagina, y + rowHeight, paintLineas)
                y += rowHeight
            }

            // 5. RESUMEN FINAL
            if (conDinero) {
                y += 20f
                if (y > 700f) {
                    doc.finishPage(page)
                    page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, doc.pages.size + 1).create())
                    canvas = page.canvas; y = 50f
                }

                val wBox = 200f
                val xBox = margen + anchoPagina - wBox

                val pResumenTitle = Paint().apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK; isAntiAlias=true }
                val pResumenVal = Paint().apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT; isAntiAlias=true }

                canvas.drawText("RESUMEN DEL PERIODO", xBox, y, pResumenTitle)
                y += 20f

                canvas.drawText("Total Ganado:", xBox, y, textPaint)
                canvas.drawText("$${String.format("%.2f", granTotal)}", xBox + wBox, y, pResumenVal)
                y += 20f

                canvas.drawText("(-) Material:", xBox, y, textPaint)
                pResumenVal.color = Color.parseColor("#DC2626") // Rojo
                canvas.drawText("-$${String.format("%.2f", gastoMaterial)}", xBox + wBox, y, pResumenVal)
                y += 10f

                canvas.drawLine(xBox, y, xBox + wBox, y, Paint().apply { color = Color.BLACK })
                y += 20f

                val utilidad = granTotal - gastoMaterial
                canvas.drawText("UTILIDAD NETA:", xBox, y, pResumenTitle)
                pResumenVal.color = if (utilidad >= 0) Color.parseColor("#16A34A") else Color.RED
                pResumenVal.textSize = 12f
                canvas.drawText("$${String.format("%.2f", utilidad)}", xBox + wBox, y, pResumenVal)
            }

            doc.finishPage(page)
            val fos = FileOutputStream(file); doc.writeTo(fos); fos.close()
            compartirArchivo(file)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            try { doc.close() } catch (e: Exception) {}
        }
    }

    private fun measureTextHeight(text: String, width: Float, paint: TextPaint): Float {
        if (text.isEmpty()) return 0f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1f, 1f)
            .setIncludePad(false)
            .build()
        return layout.height.toFloat()
    }

    // UPDATED: Now supports explicit alignment
    private fun drawMultiline(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, width: Float, paint: TextPaint, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL) {
        if (text.isEmpty()) return
        canvas.save()
        canvas.translate(x, y)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(align)
            .setLineSpacing(1f, 1f)
            .setIncludePad(false)
            .build()
        layout.draw(canvas)
        canvas.restore()
    }

    // --- CSV ---
    private fun generarExcelCSV() {
        if (listaActual.isEmpty()) {
            Toast.makeText(context, "No hay datos.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reporte_${System.currentTimeMillis()}.csv")
        try {
            val writer = java.io.PrintStream(file)
            writer.print("\uFEFF")
            writer.println("ORDEN,AUTO,COLOR,PLACA,FECHA,ESTADO,TOTAL ($),DETALLES,NOTAS")
            for (item in listaActual) {
                var totalDinero = 0.0
                val detalles = StringBuilder()
                (item.conceptos ?: emptyList()).forEach {
                    val m = it.monto?.toDoubleOrNull() ?: 0.0
                    val p = it.porcentaje?.toDoubleOrNull() ?: 0.0
                    val g = m * (p/100)
                    totalDinero += g
                    detalles.append("[${it.nombre}:$${String.format("%.2f",g)}] ")
                }
                val notas = (item.observacion ?: "").replace(",", " ").replace("\n", " ")
                writer.println("${item.orden},${item.auto},${item.color},${item.placa},${item.fecha},${if(item.finalizado)"FINALIZADO" else "PENDIENTE"},${String.format("%.2f", totalDinero)},$detalles,$notas")
            }
            writer.close()
            compartirArchivo(file)
        } catch (e: Exception) { }
    }

    private fun compartirArchivo(file: File) {
        try {
            if(!file.exists()) return
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if(file.name.endsWith(".csv")) "text/csv" else "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir Reporte"))
        } catch (e: Exception) { }
    }
}