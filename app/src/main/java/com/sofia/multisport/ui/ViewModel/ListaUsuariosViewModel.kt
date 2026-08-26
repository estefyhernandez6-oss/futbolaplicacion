package com.sofia.multisport.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.UsuarioApp

class ListaUsuariosViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var usuariosList by mutableStateOf<List<UsuarioApp>>(emptyList())
        private set

    var cargando by mutableStateOf(true)
        private set

    init {
        obtenerUsuarios()
    }

    private fun obtenerUsuarios() {
        db.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", "Error al leer usuarios: ${error.message}")
                    cargando = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UsuarioApp::class.java)?.copy(id = doc.id)
                    }
                    usuariosList = lista
                    cargando = false
                }
            }
    }
}