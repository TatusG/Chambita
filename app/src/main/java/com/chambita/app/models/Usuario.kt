package com.chambita.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Modelo de datos unificado para Firestore.
 * @IgnoreExtraProperties evita errores si el documento en Firestore tiene más campos
 * de los declarados aquí.
 */
@IgnoreExtraProperties
data class Usuario(
    // --- CAMPOS COMPARTIDOS ---
    val uid: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val dni: String = "",
    val telefono: String = "",
    val rol: String = "", // "cliente" o "tecnico"
    val fotoPerfil: String = "",
    val fcmToken: String = "", // Faltaba en tu código original
    val notificacionesHabilitadas: Boolean = true,
    val estaEnLinea: Boolean = false, // ✅ NUEVO: Para estado real
    val ultimaConexion: Timestamp? = null, // ✅ NUEVO: Para saber cuándo estuvo activo
    val fechaRegistro: Timestamp? = null,

    // --- CAMPOS EXCLUSIVOS: CLIENTE ---
    val distritoResidencia: String = "",
    val fechaNacimiento: Timestamp? = null,

    // --- CAMPOS EXCLUSIVOS: TÉCNICO ---
    val disponible: Boolean = false,
    val distritoActivoHoy: String = "",
    val especialidad: String = "",
    val tarifaPorHora: Double = 0.0,
    val tarifaMaxima: Double = 0.0, // Rango superior de precio
    val descripcion: String = "",
    val experienciaAnos: Int = 0,
    val promedioEstrellas: Double = 0.0,
    val numeroResenas: Int = 0,
    val conteoTrabajos: Int = 0,
    val servicios: List<String> = emptyList(),
    val distritos: List<String> = emptyList()
)