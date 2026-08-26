package com.sofia.multisport.ViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.Equipo

class ListaEquiposViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var equiposList by mutableStateOf<List<Equipo>>(emptyList())
        private set

    var cargando by mutableStateOf(true)
        private set

    init {
        obtenerEquipos()
    }

    private fun obtenerEquipos() {
        db.collection("equipos_globales")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    cargando = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Equipo::class.java)?.copy(id = doc.id)
                    }
                    equiposList = lista
                    cargando = false
                }
            }
    }
}