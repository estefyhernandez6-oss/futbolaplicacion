package com.sofia.multisport.ui.screens


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TablaPosicionesPantallaTest(
    deporteId: String = "futbol",
    modifier: Modifier = Modifier
) {
    Text(text = "Prueba de pantalla: $deporteId", modifier = modifier)
}