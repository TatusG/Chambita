package com.chambita.app.models

import com.google.firebase.Timestamp

data class Mensaje(
    val id: String = "",
    val remitenteId: String = "",
    val texto: String = "",
    val tipo: String = "texto", // "texto" o "imagen"
    val leido: Boolean = false,
    val fechaRegistro: Timestamp? = null
)
