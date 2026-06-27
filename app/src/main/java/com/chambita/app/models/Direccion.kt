package com.chambita.app.models

import com.google.firebase.Timestamp

data class Direccion(
    val id: String = "",
    val alias: String = "",
    val direccion: String = "",
    val distrito: String = "",
    val referencia: String = "",
    val esPrincipal: Boolean = false,
    val fechaRegistro: Timestamp? = null
)
