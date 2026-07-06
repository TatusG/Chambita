package com.chambita.app.data.repository

import com.chambita.app.models.Solicitud
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class SolicitudRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Escucha solicitudes para el técnico:
     * 1. Asignadas directamente a él (tecnicoId == uid) y pendientes.
     * 2. Abiertas (tecnicoId == null) en sus distritos y pendientes.
     * ✅ Se ordena en memoria para evitar requerir índices compuestos de Firestore.
     */
    fun escucharSolicitudesParaTecnico(
        uid: String,
        distritos: List<String>,
        onUpdate: (List<Solicitud>) -> Unit
    ): ListenerRegistration {
        return db.collection("solicitudes")
            .whereEqualTo("estado", "pendiente")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Filtrado y ordenamiento en memoria
                val filtradas = lista.filter { sol ->
                    sol.tecnicoId == uid || (sol.tecnicoId == null && distritos.contains(sol.distritoServicio))
                }.sortedByDescending { it.fechaCreacion }
                
                onUpdate(filtradas)
            }
    }

    /**
     * El técnico acepta el trabajo: se asigna su UID y cambia el estado.
     */
    fun aceptarSolicitud(solicitudId: String, tecnicoId: String, onSuccess: () -> Unit) {
        if (solicitudId.isEmpty()) return
        
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
        if (solicitudId.isEmpty()) return

        db.collection("solicitudes").document(solicitudId)
            .update("estado", "finalizada")
            .addOnSuccessListener { onSuccess() }
    }
}
