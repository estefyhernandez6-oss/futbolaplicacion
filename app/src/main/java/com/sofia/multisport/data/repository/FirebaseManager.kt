package com.sofia.multisport.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirebaseManager {
    private val db = FirebaseFirestore.getInstance()

    // 1. Iniciar un campeonato (Ruta raíz o ajustada a tu estructura de campeonatos)
    //
    // Con `update` la operacion fallaba entera si el documento del campeonato aun no
    // existia, y el motivo se perdia en un `onResult(false)` mudo. `set` con merge
    // escribe igual sobre un documento nuevo y solo toca el campo indicado.
    fun comenzarCampeonato(campeonatoId: String, onResult: (Boolean) -> Unit) {
        db.collection("campeonatos").document(campeonatoId)
            .set(mapOf("estado" to "En Progreso"), SetOptions.merge())
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e ->
                Log.e("GUARDADO", "campeonatos/$campeonatoId no se pudo iniciar", e)
                onResult(false)
            }
    }

    // 2. Actualizar eventos en tiempo real usando la ruta raíz "partidos_en_vivo"
    fun actualizarMarcadorEnVivo(
        partidoId: String,
        golesLocal: Int,
        golesVisitante: Int,
        ultimoEvento: String
    ) {
        val datosActualizados = mapOf(
            "puntuacionLocal" to golesLocal,
            "puntuacionVisitante" to golesVisitante,
            "eventoReciente" to ultimoEvento,
            "ultimaActualizacion" to System.currentTimeMillis()
        )

        db.collection("partidos_en_vivo").document(partidoId)
            .set(datosActualizados, SetOptions.merge())
            .addOnSuccessListener { Log.d("FIREBASE_LIVE", "Marcador actualizado con éxito") }
            .addOnFailureListener { e -> Log.e("FIREBASE_LIVE", "Error al actualizar", e) }
    }

    // 3. Escuchar en tiempo real en la ruta raíz
    fun escucharPartidoDestacado(
        partidoId: String,
        onCambioDetectado: (Map<String, Any>?) -> Unit
    ) {
        db.collection("partidos_en_vivo").document(partidoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LIVE", "Error al escuchar tiempo real", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    onCambioDetectado(snapshot.data)
                }
            }
    }
}