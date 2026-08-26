package com.sofia.multisport.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sofia.multisport.data.models.*
import com.sofia.multisport.ui.components.SeccionPublicidad

@Composable
fun HomeUsuarioPantalla(
    onNavegarARegistro: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var listaPublicidades by remember { mutableStateOf(emptyList<Publicidad>()) }
    var deporteSeleccionado by remember { mutableStateOf("futbol") }

    var listaPartidosEnVivo by remember { mutableStateOf(emptyList<Partido>()) }
    var partidoEnVivoSeleccionado by remember { mutableStateOf<Partido?>(null) }

    var proximosPartidos by remember { mutableStateOf(emptyList<Partido>()) }
    var listaNoticias by remember { mutableStateOf(emptyList<Noticia>()) }

    LaunchedEffect(deporteSeleccionado) {
        // Estas cuatro consultas descartaban el error con `_`. Las dos primeras necesitan
        // un indice compuesto (ver firestore.indexes.json): sin el, Firestore devuelve
        // FAILED_PRECONDITION con el enlace para crearlo, y la pantalla salia vacia sin
        // decir por que. Ahora el motivo queda en el Logcat con la etiqueta LECTURA.
        db.collection("partidos_en_vivo")
            .whereEqualTo("deporteId", deporteSeleccionado)
            .whereIn("estado", listOf("En Curso", "Suspendido"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LECTURA", "partidos en vivo: ${error.message}", error)
                    return@addSnapshotListener
                }
                val enVivo = snapshot?.toObjects(Partido::class.java) ?: emptyList()
                listaPartidosEnVivo = enVivo

                partidoEnVivoSeleccionado = when {
                    enVivo.isEmpty() -> null
                    partidoEnVivoSeleccionado == null || enVivo.none { it.id == partidoEnVivoSeleccionado?.id } -> enVivo.firstOrNull()
                    else -> enVivo.find { it.id == partidoEnVivoSeleccionado?.id }
                }
            }

        db.collection("partidos_en_vivo")
            .whereEqualTo("deporteId", deporteSeleccionado)
            .whereEqualTo("estado", "Programado")
            .orderBy("fechaHora", Query.Direction.ASCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LECTURA", "proximos partidos: ${error.message}", error)
                    return@addSnapshotListener
                }
                proximosPartidos = snapshot?.toObjects(Partido::class.java) ?: emptyList()
            }

        db.collection("noticias")
            .whereIn("deporteId", listOf(deporteSeleccionado, "global"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LECTURA", "noticias: ${error.message}", error)
                    return@addSnapshotListener
                }
                listaNoticias = snapshot?.toObjects(Noticia::class.java) ?: emptyList()
            }

        db.collection("publicidades").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("LECTURA", "publicidades: ${error.message}", error)
                return@addSnapshotListener
            }
            listaPublicidades = snapshot?.toObjects(Publicidad::class.java) ?: emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4F8))
    ) {
        MenuDeportesUltraModerno(
            deporteSeleccionado = deporteSeleccionado,
            onDeporteCambiado = { deporteSeleccionado = it }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F3460), Color(0xFF16213E), Color(0xFFE94560))
                            )
                        )
                        .clickable { onNavegarARegistro() }
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFD700)
                            ) {
                                Text(
                                    text = "  OFICIAL 2026  ",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Inscribe tu Equipo al Torneo",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gestiona plantillas y compite por la gloria ⚽",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("➔", fontSize = 20.sp, color = Color(0xFF0F3460), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (listaPartidosEnVivo.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFE94560), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EN DIRECTO",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1A1A2E)
                            )
                        }
                        Text(
                            text = "${listaPartidosEnVivo.size} en curso",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }

                    if (listaPartidosEnVivo.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaPartidosEnVivo, key = { it.id }) { partido ->
                                val esSeleccionado = partidoEnVivoSeleccionado?.id == partido.id
                                Surface(
                                    modifier = Modifier.clickable { partidoEnVivoSeleccionado = partido },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (esSeleccionado) Color(0xFF1A1A2E) else Color.White,
                                    shadowElevation = if (esSeleccionado) 4.dp else 1.dp
                                ) {
                                    Text(
                                        text = "${partido.equipoLocalNombre} vs ${partido.equipoVisitanteName()}",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        color = if (esSeleccionado) Color.White else Color.DarkGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    partidoEnVivoSeleccionado?.let { partido ->
                        CardPartidoEnVivoImponente(partido = partido)
                    }
                }
            }

            item {
                SeccionPublicidad(publicidades = listaPublicidades)
            }

            if (proximosPartidos.isNotEmpty()) {
                item {
                    Text(
                        text = "PRÓXIMOS ENCUENTROS 📅",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1A2E)
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(proximosPartidos, key = { it.id }) { partido ->
                            TarjetaProximoPartidoModerna(partido)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ACTUALIDAD DEL TORNEO 📰",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1A2E)
                )
            }

            if (listaNoticias.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "No hay noticias recientes disponibles.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(listaNoticias, key = { it.id }) { noticia ->
                    FilaNoticiaModerna(noticia = noticia)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun MenuDeportesUltraModerno(deporteSeleccionado: String, onDeporteCambiado: (String) -> Unit) {
    val deportes = listOf(
        "futbol" to "Fútbol ⚽",
        "basquet" to "Básquet 🏀",
        "voley" to "Vóley 🏐"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A2E),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            deportes.forEach { (id, nombre) ->
                val seleccionado = deporteSeleccionado == id
                Surface(
                    onClick = { onDeporteCambiado(id) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (seleccionado) Color(0xFFE94560) else Color(0xFF262A40),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nombre,
                            color = if (seleccionado) Color.White else Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardPartidoEnVivoImponente(partido: Partido) {
    val esSuspendido = partido.estado == "Suspendido"
    val contexto = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esSuspendido) Color(0xFF2C2C2C) else Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (esSuspendido) Color(0xFFFF9F43) else Color(0xFFE94560)
                    ) {
                        Text(
                            text = if (esSuspendido) " SUSPENDIDO " else " 🔴 EN VIVO ",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🏟️ ${partido.cancha}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (partido.linkTransmision.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    val url = if (!partido.linkTransmision.startsWith("http")) "https://${partido.linkTransmision}" else partido.linkTransmision
                                    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: Exception) {
                                    Toast.makeText(contexto, "Enlace no válido", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Ver", tint = Color(0xFF00F5D4))
                        }
                    }
                    IconButton(
                        onClick = {
                            val texto = "🔴 *${partido.equipoLocalNombre}* ${partido.puntuacionLocal} - ${partido.puntuacionVisitante} *${partido.equipoVisitanteName()}*\nSíguelo en MultiSport 🏆"
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto) }
                            contexto.startActivity(Intent.createChooser(intent, "Compartir"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = partido.equipoLocalNombre,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "  ${partido.puntuacionLocal} - ${partido.puntuacionVisitante}  ",
                        color = Color(0xFF00F5D4),
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = partido.equipoVisitanteName(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = partido.eventoReciente.ifBlank { "⚡ Minuto a minuto activo..." },
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            if (partido.notaOficial.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE94560).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📢", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = partido.notaOficial, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TarjetaProximoPartidoModerna(partido: Partido) {
    Card(
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📅 PRÓXIMO", color = Color(0xFF0F3460), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(text = "📍 ${partido.cancha}", color = Color.Gray, fontSize = 10.sp, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center, // Corregido aquí
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = partido.equipoLocalNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
                Text(text = " VS ", fontWeight = FontWeight.Black, color = Color.LightGray, modifier = Modifier.padding(horizontal = 6.dp), fontSize = 11.sp)
                Text(text = partido.equipoVisitanteName(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${partido.fechaString} • ${partido.horaString}", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FilaNoticiaModerna(noticia: Noticia) {
    val contexto = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFEFF6FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = noticia.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B), maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = noticia.contenido, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(
                onClick = {
                    val texto = "⚽ *${noticia.titulo}*\n\n${noticia.contenido}\n\n_Vía MultiSport Manager_"
                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto) }
                    contexto.startActivity(Intent.createChooser(intent, "Compartir noticia"))
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun Partido.equipoVisitanteName(): String {
    return try {
        this.equipoVisitanteNombre
    } catch (_: Exception) {
        ""
    }
}