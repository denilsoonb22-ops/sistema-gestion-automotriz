package com.destajo.pintura

import java.io.Serializable

data class Concepto(
    val nombre: String = "",
    val monto: String = "0",
    val porcentaje: String = "0"
) : Serializable

data class Registro(
    var id: String = "",
    val auto: String = "",
    val color: String = "",
    val orden: String = "",
    val placa: String = "",
    val observacion: String = "",
    val finalizado: Boolean = false,
    val fecha: String = "", // Formato YYYY-MM-DD
    val createdAt: Any? = null, // Timestamp de Firebase
    val conceptos: List<Concepto> = emptyList()
) : Serializable