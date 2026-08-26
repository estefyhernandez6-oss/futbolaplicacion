package com.sofia.multisport.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.Partido

class ArbitroEnVivoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var partidosEnVivo by mutableStateOf<List<Partido>>(emptyList())
        private set

    var cargando by mutableStateOf(true)
        private set

    init {
        escucharPartidosEnVivo()
    }

    private fun escucharPartidosEnVivo() {
        db.collection("partidos_en_vivo")
            .whereIn("estado", listOf("En Curso", "Suspendido"))
            .addSnapshotListener { snapshot, error ->
                cargando = false
                if (error != null) {
                    Log.e("LECTURA", "partidos en vivo: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    partidosEnVivo = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Partido::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }
}