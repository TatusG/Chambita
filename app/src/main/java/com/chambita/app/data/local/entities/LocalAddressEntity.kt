package com.chambita.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_addresses")
data class LocalAddressEntity(
    @PrimaryKey val direccionId: String,
    val clienteId: String,
    val alias: String,
    val direccion: String,
    val distrito: String,
    val esPrincipal: Boolean
)
