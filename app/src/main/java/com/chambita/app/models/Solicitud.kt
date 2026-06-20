package com.chambita.app.models

import com.google.firebase.Timestamp

data class Solicitud(
    val id: String = "",
    val clienteId: String = "",
    val tecnicoId: String? = null,
    val descripcionAveria: String = "",
    val especialidadRequerida: String = "",
    val fechaCreacion: Timestamp? = null,
    val fechaServicioProgramado: Timestamp? = null,
    val direccionServicio: String = "",
    val estado: String = "pendiente",
    val montoFinal: Double = 0.0,
    val resenaDejada: Boolean = false,
    val nombreCliente: String = "",
    val fotoCliente: String = ""
)
