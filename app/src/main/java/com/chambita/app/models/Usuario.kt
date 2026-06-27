package com.chambita.app.models

import com.google.firebase.Timestamp

data class Usuario(
    val uid: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val dni: String = "",
    val telefono: String = "",
    val rol: String = "", // "cliente" o "tecnico"
    val fotoPerfil: String = "",
    val fechaRegistro: Timestamp? = null,
    
    // Campos específicos para Clientes
    val distritoResidencia: String = "",
    val fechaNacimiento: Timestamp? = null,
    
    // Campos específicos para Técnicos
    val disponible: Boolean = false,
    val distritoActivoHoy: String = "",
    val especialidad: String = "",
    val tarifaPorHora: Double = 0.0,
    val descripcion: String = "",
    val experienciaAnos: Int = 0,
    val promedioEstrellas: Double = 0.0,
    val numeroResenas: Int = 0,
    val conteoTrabajos: Int = 0,
    val servicios: List<String> = emptyList(),
    val distritos: List<String> = emptyList()
)
