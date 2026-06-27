package com.chambita.app.models

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "", // El ID del documento será "{clienteId}_{tecnicoId}"
    val clienteId: String = "",
    val tecnicoId: String = "",
    val ultimoMensaje: String = "",
    val fechaUltimoMensaje: Timestamp? = null
)
