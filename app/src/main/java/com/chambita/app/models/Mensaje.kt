package com.chambita.app.models

import com.google.firebase.Timestamp

data class Mensaje(
    val id: String = "",
    val remitenteId: String = "",
    val texto: String = "",
    val leido: Boolean = false,
    val fechaRegistro: Timestamp? = null
)
