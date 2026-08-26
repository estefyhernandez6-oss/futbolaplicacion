package com.sofia.multisport.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.sofia.multisport.data.models.FilaGoleador

/**
 * Tabla de goleadores.
 *
 * Leia los documentos como `Goleador` (campos `nombre`, `equipoNombre`), pero el panel
 * de arbitraje escribe `FilaGoleador` (campos `jugadorNombre`, `equipoNombre`, ...).
 * Los nombres nunca coincidian, asi que la columna "Jugador" salia en blanco aunque
 * los goles estuvieran bien guardados.
 */
@Composable
fun TablaGoleadoresPantalla(
    deporteId: String = "futbol",
    listaGoleadores: List<FilaGoleador> = emptyList(),
    modifier: Modifier = Modifier
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var goleadores by remember { mutableStateOf(listaGoleadores) }
    var cargando by remember { mutableStateOf(true) }

    // Cargar goleadores desde Firestore en tiempo real
    LaunchedEffect(deporteId) {
        db.collection("goleadores")
            .whereEqualTo("deporteId", deporteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LECTURA", "goleadores: ${error.message}", error)
                    cargando = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lista = snapshot.toObjects(FilaGoleador::class.java)
                    // Ordenar de mayor a menor según los goles
                    goleadores = lista.sortedByDescending { it.goles }
                }
                cargando = false
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(16.dp)
    ) {
        Text(
            text = "TABLA DE GOLEADORES",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Encabezado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(28.dp))
                Text("Jugador", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Equipo", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Goles", color = Color(0xFF00F5D4), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
            }
        }

        when {
            cargando -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1A1A2E))
                }
            }
            goleadores.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay goleadores registrados todavía.", color = Color.Gray, fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White)
                ) {
                    itemsIndexed(goleadores) { indice, goleador ->
                        val colorFondo = if (indice % 2 == 0) Color.White else Color(0xFFF8F9FA)

                        Column(modifier = Modifier.fillMaxWidth().background(colorFondo)) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${indice + 1}",
                                    color = if (indice == 0) Color(0xFFE94560) else Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = goleador.jugadorNombre,
                                    color = Color.Black,
                                    fontWeight = if (indice == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    text = goleador.equipoNombre,
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${goleador.goles}",
                                    color = Color(0xFF1A1A2E),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.width(44.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}