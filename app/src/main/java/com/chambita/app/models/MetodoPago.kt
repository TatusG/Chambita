package com.chambita.app.models

import com.google.firebase.Timestamp

data class MetodoPago(
    val id: String = "",
    val tipo: String = "", // "Yape", "Plin", "Efectivo"
    val numeroAsociado: String = "",
    val esPredeterminado: Boolean = false,
    val fechaRegistro: Timestamp? = null
)
