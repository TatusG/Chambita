package com.chambita.app.models

import com.google.firebase.Timestamp

data class Mensaje(
    val remitenteId: String = "",
    val texto: String = "",
    val tipo: String = "texto",
    val monto: Double = 0.0,
    val leido: Boolean = false,
    val fechaRegistro: Timestamp? = null,
    val estadoPropuesta: String = "pendiente"
)
