package com.chambita.app.data.repository

import com.chambita.app.models.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TecnicoRepository {

    private val db = FirebaseFirestore.getInstance()

    fun buscarTodosLosTecnicosPorDistrito(
        distrito: String,
        onSuccess: (List<Usuario>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)
            .whereArrayContains("distritos", distrito)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lista = querySnapshot.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id)
                }
                onSuccess(lista)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun buscarTecnicosPorEspecialidadYDistrito(
        especialidad: String,
        distrito: String,
        onSuccess: (List<Usuario>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)
            .whereEqualTo("especialidad", especialidad)
            .whereArrayContains("distritos", distrito)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listaTecnicos = mutableListOf<Usuario>()
                for (document in querySnapshot) {
                    try {
                        val tecnico = document.toObject(Usuario::class.java)
                        val tecnicoConUid = tecnico.copy(uid = document.id)
                        listaTecnicos.add(tecnicoConUid)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                onSuccess(listaTecnicos)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun buscarTecnicosOrdenadosPorEstrellas(
        especialidad: String,
        distrito: String,
        onSuccess: (List<Usuario>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")
            .whereEqualTo("disponible", true)
            .whereEqualTo("especialidad", especialidad)
            .whereArrayContains("distritos", distrito)
            .orderBy("promedioEstrellas", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lista = querySnapshot.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id)
                }
                onSuccess(lista)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
