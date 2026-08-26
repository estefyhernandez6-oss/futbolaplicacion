package com.sofia.multisport.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JugadoresYEquiposPantalla() {
    // Usamos un Column o pestañas internas para organizar ambos listados en una sola sección
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "⚽ Registros del Sistema",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Aquí puedes mostrar las dos pantallas que ya creaste previamente
        // (Nota: Si prefieres mostrarlas en pestañas o una debajo de otra con scroll, puedes envolverlas en un LazyColumn o Column con weight)

        Text(text = "--- Lista de Equipos Globales ---", fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ListaEquiposPantalla()
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "--- Lista de Usuarios / Jugadores ---", fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ListaUsuariosPantalla()
        }
    }
}