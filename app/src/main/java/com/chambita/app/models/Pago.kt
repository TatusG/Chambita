package com.chambita.app.models

import com.google.firebase.Timestamp

data class Pago(
    val id: String = "",
    val clienteId: String = "",
    val tecnicoId: String = "",
    val solicitudId: String = "",
    val monto: Double = 0.0,
    val metodoUsado: String = "", // e.g., "tarjeta", "yape", "plin"
    val estado: String = "pendiente", // e.g., "exitoso", "fallido"
    val fechaRegistro: Timestamp? = null
)
