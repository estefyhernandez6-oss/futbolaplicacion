package com.sofia.multisport.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sofia.multisport.data.models.FilaGoleador

/**
 * Ranking de goleadores con podio y filas pulsables.
 *
 * Se llamaba `TablaGoleadoresPantalla`, el mismo nombre que el composable de
 * TablaGoleadoresPantalla.kt y en el mismo paquete: dos declaraciones compitiendo por
 * la misma llamada. Renombrado al nombre de su propio archivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingGoleadoresPantalla(
    deporteId: String,
    listaGoleadores: List<FilaGoleador>,
    modifier: Modifier = Modifier,
    onGoleadorSeleccionado: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .padding(16.dp)
    ) {
        // Cabecera estilizada
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TABLA DE GOLEADORES",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Máximos anotadores del torneo",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Estado vacío o lista
        if (listaGoleadores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay registros de goleadores.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(
                    items = listaGoleadores,
                    key = { index, goleador -> goleador.jugadorId.ifEmpty { index.toString() } }
                ) { index, goleador ->

                    // Colores de medalla para el Top 3
                    val (badgeColor, textColor) = when (index) {
                        0 -> Color(0xFFFFD700) to Color.Black // Oro
                        1 -> Color(0xFFC0C0C0) to Color.Black // Plata
                        2 -> Color(0xFFCD7F32) to Color.White // Bronce
                        else -> Color(0xFFE9ECEF) to Color(0xFF1A1A2E) // Resto
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onGoleadorSeleccionado(goleador.jugadorId) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Círculo con posición / número
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Nombre del jugador y equipo
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goleador.jugadorNombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1A1A2E)
                                )
                                Text(
                                    text = goleador.equipoNombre,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // Badge de goles estilizado
                            Surface(
                                color = Color(0xFFE94560).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${goleador.goles} G",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE94560),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Ver detalles",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}