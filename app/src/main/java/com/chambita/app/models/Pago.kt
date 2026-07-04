package com.chambita.app.models

import com.google.firebase.Timestamp

data class Pago(
    val id: String = "",
    val clienteId: String = "",
    val tecnicoId: String = "",
    val solicitudId: String = "",
    val monto: Double = 0.0,
    val metodoUsado: String = "",
    val estado: String = "exitoso", // exitoso, pendiente, fallido
    val fechaRegistro: Timestamp? = null
)
