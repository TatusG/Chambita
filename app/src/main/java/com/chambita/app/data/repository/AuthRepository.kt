package com.chambita.app.data.repository

import com.chambita.app.data.local.dao.UserSessionDao
import com.chambita.app.data.local.entities.UserSessionEntity
import com.chambita.app.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val sessionDao: UserSessionDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun login(correo: String, contrasena: String): Result<Usuario> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(correo, contrasena).await()
            val uid = authResult.user?.uid ?: throw Exception("Error al obtener UID")

            val doc = db.collection("usuarios").document(uid).get().await()
            val usuario = doc.toObject(Usuario::class.java) ?: throw Exception("Perfil no encontrado")

            sessionDao.clearActiveSessions()
            val session = UserSessionEntity(
                uid = uid,
                nombreCompleto = usuario.nombreCompleto,
                correo = usuario.correo,
                rol = usuario.rol,
                fotoPerfil = usuario.fotoPerfil,
                fcmToken = usuario.fcmToken,
                estaActivo = true
            )
            sessionDao.insertSession(session)

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
        sessionDao.clearActiveSessions()
    }

    /**
     * Envía un correo de recuperación de contraseña.
     */
    suspend fun recuperarContrasena(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveSession(): UserSessionEntity? {
        return sessionDao.getActiveSession()
    }
}
