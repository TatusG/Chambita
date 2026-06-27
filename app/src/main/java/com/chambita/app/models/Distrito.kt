package com.chambita.app.models

import com.google.firebase.firestore.GeoPoint

data class Distrito(
    val id: String = "", // El ID del documento será el Nombre del distrito (ej. "Ventanilla")
    val nombre: String = "",
    val codigoPostal: String = "",
    val coordenadas: GeoPoint? = null,
    val distritosVecinos: List<String> = emptyList()
)
