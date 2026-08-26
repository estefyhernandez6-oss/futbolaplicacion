package com.sofia.multisport.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.Campeonato
import com.sofia.multisport.data.models.Equipo
import com.sofia.multisport.data.models.FilaPosicion
import com.sofia.multisport.data.models.Publicidad
import com.sofia.multisport.ui.components.SeccionPublicidad

@Composable
fun TablaPosicionesPantalla(
    modifier: Modifier = Modifier,
    deporteId: String = "futbol"
) {
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current
    var listaPosiciones by remember { mutableStateOf(emptyList<FilaPosicion>()) }
    var listaPublicidades by remember { mutableStateOf(emptyList<Publicidad>()) }

    var listaCampeonatos by remember { mutableStateOf(emptyList<Campeonato>()) }
    var campeonatoSeleccionado by remember { mutableStateOf<Campeonato?>(null) }
    var expCamp by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }

    // 1. Cargar Campeonatos y Publicidad
    LaunchedEffect(deporteId) {
        db.collection("campeonatos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val todos = snapshot.toObjects(Campeonato::class.java)
                    listaCampeonatos = todos
                    if (campeonatoSeleccionado == null && todos.isNotEmpty()) {
                        campeonatoSeleccionado = todos.first()
                    }
                }
            }

        db.collection("publicidades").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                listaPublicidades = snapshot.toObjects(Publicidad::class.java)
            }
        }
    }

    // 2. Escuchar los datos de la tabla de posiciones
    LaunchedEffect(campeonatoSeleccionado) {
        if (campeonatoSeleccionado != null) {
            cargando = true
            db.collection("tabla_posiciones")
                .whereEqualTo("campeonatoId", campeonatoSeleccionado!!.id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        cargando = false
                        return@addSnapshotListener
                    }
                    val stats = snapshot?.toObjects(FilaPosicion::class.java) ?: emptyList()

                    db.collection("equipos_globales")
                        .whereEqualTo("categoriaId", campeonatoSeleccionado!!.categoriaId)
                        .get()
                        .addOnSuccessListener { eqSnapshot ->
                            val todosLosEquipos = eqSnapshot.toObjects(Equipo::class.java)

                            val tablaCompleta = todosLosEquipos.map { equipo ->
                                stats.find { it.equipoId == equipo.id } ?: FilaPosicion(
                                    equipoId = equipo.id,
                                    equipoNombre = equipo.nombre,
                                    campeonatoId = campeonatoSeleccionado!!.id,
                                    deporteId = deporteId
                                )
                            }

                            listaPosiciones = tablaCompleta.sortedWith(
                                compareByDescending<FilaPosicion> { it.puntos }
                                    .thenByDescending { it.golDiferencia }
                            )
                            cargando = false
                        }
                        .addOnFailureListener {
                            cargando = false
                        }
                }
        } else {
            cargando = false
            listaPosiciones = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(16.dp)
    ) {
        Text(
            text = "TABLA DE POSICIONES",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1A1A2E)
        )

        // Selector de Campeonato
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            OutlinedButton(
                onClick = { expCamp = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Text(
                    text = campeonatoSeleccionado?.nombre ?: "🏆 Seleccionar Torneo...",
                    fontWeight = FontWeight.SemiBold
                )
            }
            DropdownMenu(
                expanded = expCamp,
                onDismissRequest = { expCamp = false }
            ) {
                listaCampeonatos.forEach { camp ->
                    DropdownMenuItem(
                        text = { Text("${camp.nombre} (${camp.categoriaId.uppercase()})") },
                        onClick = {
                            campeonatoSeleccionado = camp
                            expCamp = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Clasificación oficial de ${campeonatoSeleccionado?.nombre ?: "el torneo"}.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Botón Compartir Informe
        Button(
            onClick = {
                if (listaPosiciones.isNotEmpty()) {
                    compartirInforme(contexto, campeonatoSeleccionado?.nombre, listaPosiciones)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4), contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Compartir Informe 📋", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Encabezado de la Tabla
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("Club", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))

                val headers = listOf("PJ", "GD", "PTS")
                val anchos = listOf(32.dp, 36.dp, 40.dp)

                headers.forEachIndexed { index, text ->
                    Text(
                        text = text,
                        color = if (text == "PTS") Color(0xFF00F5D4) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.width(anchos[index]),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cuerpo de la Tabla
        if (cargando) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1A1A2E))
            }
        } else if (listaPosiciones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay datos registrados para este torneo todavía.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = listaPosiciones,
                    key = { index, fila -> fila.equipoId.ifEmpty { index.toString() } }
                ) { indice, fila ->
                    val esPuntero = indice == 0

                    // Reemplazamos la fila plana por un Card elegante
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${indice + 1}",
                                    color = if (esPuntero) Color(0xFFE94560) else Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = fila.equipoNombre,
                                    color = Color.Black,
                                    fontWeight = if (esPuntero) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )

                                Text(text = "${fila.partidosJugados}", color = Color.Black, fontSize = 13.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)

                                val colorGD = when {
                                    fila.golDiferencia > 0 -> Color(0xFF2ECC71)
                                    fila.golDiferencia < 0 -> Color(0xFFE74C3C)
                                    else -> Color.DarkGray
                                }
                                val signoGD = if (fila.golDiferencia > 0) "+${fila.golDiferencia}" else "${fila.golDiferencia}"

                                Text(text = signoGD, color = colorGD, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)

                                Text(
                                    text = "${fila.puntos}",
                                    color = Color(0xFF1A1A2E),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Historial de Partidos (G/E/P)
                            if (fila.historial.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 44.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    fila.historial.forEach { resultado ->
                                        val colorCirculo = when(resultado) {
                                            "G" -> Color(0xFF2ECC71)
                                            "E" -> Color(0xFFF1C40F)
                                            "P" -> Color(0xFFE74C3C)
                                            else -> Color.Gray
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(colorCirculo, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(resultado, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SeccionPublicidad(publicidades = listaPublicidades)
    }
}

private fun compartirInforme(context: Context, nombreTorneo: String?, posiciones: List<FilaPosicion>) {
    val sb = StringBuilder()
    sb.append("🏟️ *ARENA LIVE - REPORTE OFICIAL*\n")
    sb.append("🏆 *${nombreTorneo?.uppercase() ?: "TORNEO"}*\n")
    sb.append("-------------------------------------------\n")
    posiciones.forEachIndexed { i, f ->
        val gd = if (f.golDiferencia > 0) "+${f.golDiferencia}" else "${f.golDiferencia}"
        sb.append("${i + 1}. ${f.equipoNombre} | PJ:${f.partidosJugados} | GD:$gd | PTS:${f.puntos}\n")
    }
    sb.append("-------------------------------------------\n")
    sb.append("_Generado por Arena Live App_")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Compartir Tabla"))
}