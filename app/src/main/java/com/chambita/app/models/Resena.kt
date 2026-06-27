package com.chambita.app.models

import com.google.firebase.Timestamp

data class Resena(
    val id: String = "",
    val clienteId: String = "",
    val nombreCliente: String = "",
    val calificacion: Int = 0,
    val comentario: String = "",
    val recomienda: Boolean = false,
    val solicitudId: String = "",
    val fechaRegistro: Timestamp? = null
)
