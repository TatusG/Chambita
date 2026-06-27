package com.chambita.app.models

import com.google.firebase.Timestamp

data class MetodoPago(
    val id: String = "",
    val tipo: String = "", // e.g., "tarjeta", "yape", "plin"
    val numeroAsociado: String = "",
    val esPredeterminado: Boolean = false,
    val fechaRegistro: Timestamp? = null
)
