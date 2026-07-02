package com.chambita.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chambita.app.data.local.entities.LocalAddressEntity

@Dao
interface LocalAddressDao {
    @Query("SELECT * FROM local_addresses WHERE clienteId = :clienteId")
    suspend fun getAddressesByClient(clienteId: String): List<LocalAddressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: LocalAddressEntity)

    @Query("DELETE FROM local_addresses WHERE direccionId = :id")
    suspend fun deleteAddress(id: String)
}
