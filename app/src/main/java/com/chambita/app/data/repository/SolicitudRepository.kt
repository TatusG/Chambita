package com.chambita.app.data.repository

import com.chambita.app.models.Solicitud
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class SolicitudRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Escucha solicitudes pendientes en los distritos que cubre el técnico.
     */
    fun escucharSolicitudesNuevas(
        distritos: List<String>,
        onUpdate: (List<Solicitud>) -> Unit
    ): ListenerRegistration {
        return db.collection("solicitudes")
            .whereEqualTo("estado", "pendiente")
            .whereIn("distritoServicio", distritos)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val lista = snapshot?.toObjects(Solicitud::class.java) ?: emptyList()
                onUpdate(lista)
            }
    }

    /**
     * El técnico acepta el trabajo: se asigna su UID y cambia el estado.
     */
    fun aceptarSolicitud(solicitudId: String, tecnicoId: String, onSuccess: () -> Unit) {
        db.collection("solicitudes").document(solicitudId)
            .update(mapOf(
                "tecnicoId" to tecnicoId,
                "estado" to "aceptada"
            ))
            .addOnSuccessListener { onSuccess() }
    }

    /**
     * El técnico finaliza el trabajo.
     */
    fun finalizarSolicitud(solicitudId: String, onSuccess: () -> Unit) {
        db.collection("solicitudes").document(solicitudId)
            .update("estado", "finalizada")
            .addOnSuccessListener { onSuccess() }
    }
}
