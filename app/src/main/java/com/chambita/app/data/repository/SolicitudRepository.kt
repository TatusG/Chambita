package com.chambita.app.data.repository

import android.util.Log
import com.chambita.app.models.Solicitud
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.Normalizer

class SolicitudRepository {
    private val TAG = "SOLICITUD_REPOSITORY"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Escucha solicitudes para el técnico:
     * 1. Asignadas directamente a él (tecnicoId == uid).
     * 2. Abiertas (tecnicoId null o vacío) en sus distritos.
     */
    fun escucharSolicitudesParaTecnico(
        uid: String,
        distritos: List<String>,
        onUpdate: (List<Solicitud>) -> Unit
    ): ListenerRegistration {
        Log.d(TAG, "Iniciando escucha de solicitudes para Técnico $uid en distritos: $distritos")
        return db.collection("solicitudes")
            .whereEqualTo("estado", "pendiente")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Falla en addSnapshotListener de solicitudes:", e)
                    return@addSnapshotListener
                }
                
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Solicitud::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d(TAG, "Total solicitudes pendientes leídas de Firestore: ${lista.size}")

                val filtradas = lista.filter { sol ->
                    val esParaMi = sol.tecnicoId == uid
                    val esAbierta = sol.tecnicoId.isNullOrEmpty()
                    val estaEnMiZona = distritos.any { d ->
                        val match = matchesDistrict(d, sol.distritoServicio)
                        if (match) {
                            Log.d(TAG, "Solicitud ${sol.id} coincide con el distrito del técnico: ${sol.distritoServicio}")
                        }
                        match
                    }
                    esParaMi || (esAbierta && estaEnMiZona)
                }.sortedByDescending { it.fechaCreacion }
                
                Log.d(TAG, "Solicitudes filtradas enviadas a la UI: ${filtradas.size}")
                onUpdate(filtradas)
            }
    }

    private fun matchesDistrict(d1: String, d2: String?): Boolean {
        if (d2 == null) return false
        val n1 = normalizarTexto(d1)
        val n2 = normalizarTexto(d2)
        if (n1 == n2) return true
        
        // Manejar "Callao" y "Callao (Cercado)"
        if (n1.contains("callao") && n2.contains("callao")) return true
        
        // Manejar "Cercado de Lima" y "Lima (Cercado)"
        if (n1.contains("lima") && n1.contains("cercado") && n2.contains("lima") && n2.contains("cercado")) return true
        
        return false
    }

    private fun normalizarTexto(texto: String?): String {
        if (texto == null) return ""
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("-", " ")
            .lowercase().trim()
    }

    fun aceptarSolicitud(solicitudId: String, tecnicoId: String, onSuccess: () -> Unit) {
        if (solicitudId.isEmpty()) return
        db.collection("solicitudes").document(solicitudId)
            .update(mapOf("tecnicoId" to tecnicoId, "estado" to "aceptada"))
            .addOnSuccessListener { onSuccess() }
    }

    fun finalizarSolicitud(solicitudId: String, onSuccess: () -> Unit) {
        if (solicitudId.isEmpty()) return
        db.collection("solicitudes").document(solicitudId)
            .update("estado", "finalizada")
            .addOnSuccessListener { onSuccess() }
    }
}
