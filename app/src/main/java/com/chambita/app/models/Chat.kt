package com.chambita.app.models

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val clienteId: String = "",
    val tecnicoId: String = "",
    val ultimoMensaje: String = "",
    val fechaUltimoMensaje: com.google.firebase.Timestamp? = null
)
