package com.chambita.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chambita.app.data.local.entities.UserSessionEntity

/**
 * Data Access Object (DAO) para gestionar la sesión local del usuario.
 */
@Dao
interface UserSessionDao {

    @Query("SELECT * FROM user_session WHERE estaActivo = 1 LIMIT 1")
    suspend fun getActiveSession(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSessionEntity)

    @Query("UPDATE user_session SET estaActivo = 0")
    suspend fun clearActiveSessions()

    @Query("DELETE FROM user_session")
    suspend fun deleteSession()
}
