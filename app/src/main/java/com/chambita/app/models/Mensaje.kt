package com.chambita.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Mensaje(
    @get:Exclude var id: String = "", // Document ID
    val remitenteId: String = "",
    val texto: String = "",
    val tipo: String = "texto",
    val monto: Double = 0.0,
    val leido: Boolean = false,
    val fechaRegistro: Timestamp? = null,
    val estadoPropuesta: String = "pendiente"
)
