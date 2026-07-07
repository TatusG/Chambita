package com.chambita.app.models

import com.google.firebase.Timestamp

data class Mensaje(
    val id: String = "",
    val remitenteId: String = "",
    val texto: String = "",
    val tipo: String = "texto", // "texto", "imagen", "propuesta", "pago_final"
    val monto: Double = 0.0,    // Nuevo: para llevar el precio negociado
    val leido: Boolean = false,
    val fechaRegistro: Timestamp? = null
)
