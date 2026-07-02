package com.chambita.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa la sesión del usuario persistida localmente.
 * Permite el acceso instantáneo a los datos básicos del usuario sin red.
 */
@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val uid: String,
    val nombreCompleto: String,
    val correo: String,
    val rol: String, // "cliente" o "tecnico"
    val fotoPerfil: String?,
    val fcmToken: String?,
    val estaActivo: Boolean // true si la sesión está vigente
)
