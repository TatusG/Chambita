package com.chambita.app.data.repository

import com.chambita.app.models.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TecnicoRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Busca técnicos activos y disponibles filtrando por especialidad y por el distrito que cubren.
     * 
     * @param especialidad La especialidad técnica requerida (ej. "Gasfitero", "Electricista").
     * @param distrito El nombre del distrito donde se requiere el servicio (ej. "Ventanilla").
     * @param onSuccess Callback que se ejecuta con la lista de técnicos que cumplen el criterio.
     * @param onFailure Callback que se ejecuta en caso de error.
     */
    fun buscarTecnicosPorEspecialidadYDistrito(
        especialidad: String,
        distrito: String,
        onSuccess: (List<Usuario>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Consultamos la colección raíz polimórfica 'usuarios'
        db.collection("usuarios")
            .whereEqualTo("rol", "tecnico")               // Filtramos solo usuarios que sean técnicos
            .whereEqualTo("disponible", true)             // Solo técnicos disponibles hoy
            .whereEqualTo("especialidad", especialidad)   // Filtro por especialidad
            .whereArrayContains("distritos", distrito)   // Busca si el distrito está dentro del array de distritos que cubre el técnico
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listaTecnicos = mutableListOf<Usuario>()
                for (document in querySnapshot) {
                    try {
                        val tecnico = document.toObject(Usuario::class.java)
                        // Inyectamos el ID del documento en el modelo
                        val tecnicoConUid = tecnico.copy(uid = document.id)
                        listaTecnicos.add(tecnicoConUid)
                    } catch (e: Exception) {
                        // En caso de que algún documento tenga un formato inválido
                        e.printStackTrace()
                    }
                }
                onSuccess(listaTecnicos)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Alternativa: Consulta avanzada con ordenamiento por promedio de estrellas (reputación).
     * Nota: Esta consulta requiere un índice compuesto adicional en Firebase.
     */
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
            .orderBy("promedioEstrellas", Query.Direction.DESCENDING) // Ordena de mayor a menor calificación
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
