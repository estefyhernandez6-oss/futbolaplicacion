package com.sofia.multisport.ViewModel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.FilaGoleador

class GoleadoresViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    var listaGoleadores by mutableStateOf<List<FilaGoleador>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        cargarGoleadores()
    }

    fun cargarGoleadores() {
        isLoading = true
        db.collection("goleadores")
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // El id del documento es "${campeonatoId}_${jugadorId}"; los dos
                    // valores ya viajan como campos, asi que no hace falta copiarlo.
                    val goleadoresList = snapshot.toObjects(FilaGoleador::class.java)
                    listaGoleadores = goleadoresList.sortedByDescending { it.goles }
                }
            }
    }
}