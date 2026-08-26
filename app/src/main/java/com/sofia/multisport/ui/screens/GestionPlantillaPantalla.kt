package com.sofia.multisport.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sofia.multisport.data.models.Equipo
import com.sofia.multisport.data.models.Jugador

/**
 * Gestion de plantillas.
 *
 * Esta pantalla funcionaba entera con datos inventados: tres equipos escritos a mano
 * y cuatro `JugadorMock`. "Dar de baja" solo quitaba el jugador de una lista en
 * memoria, asi que al volver a entrar reaparecia y la base nunca se enteraba. Ahora
 * lee `equipos_globales` y `jugadores_globales`, y la baja escribe `activo = false`
 * en el documento del jugador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionPlantillaPantalla() {
    val contexto = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }

    var listaEquipos by remember { mutableStateOf(emptyList<Equipo>()) }
    var equipoSeleccionado by remember { mutableStateOf<Equipo?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var listaJugadores by remember { mutableStateOf(emptyList<Jugador>()) }
    var cargandoJugadores by remember { mutableStateOf(false) }

    var jugadorParaBaja by remember { mutableStateOf<Jugador?>(null) }
    var mostrarDialogoBaja by remember { mutableStateOf(false) }

    // Paleta de colores Premium
    val colorFondo = Color(0xFF0F172A)
    val colorTarjeta = Color(0xFF1E293B)
    val colorAcento = Color(0xFF00F5D4)
    val colorRojo = Color(0xFFE94560)
    val colorTextoSecundario = Color(0xFF94A3B8)

    // Catalogo de equipos, en tiempo real.
    DisposableEffect(Unit) {
        val registro: ListenerRegistration = db.collection("equipos_globales")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LECTURA", "equipos_globales: ${error.message}", error)
                    return@addSnapshotListener
                }
                val equipos = snapshot?.toObjects(Equipo::class.java).orEmpty()
                    .filter { it.id.isNotBlank() }
                    .sortedBy { it.nombre }
                listaEquipos = equipos
                if (equipoSeleccionado == null) equipoSeleccionado = equipos.firstOrNull()
            }
        onDispose { registro.remove() }
    }

    // Nomina del equipo elegido. Se filtra `activo` en el cliente a proposito: los
    // documentos creados antes de que existiera el campo no lo traen, y una consulta
    // whereEqualTo("activo", true) los dejaria fuera.
    DisposableEffect(equipoSeleccionado?.id) {
        val equipoId = equipoSeleccionado?.id
        if (equipoId.isNullOrBlank()) {
            listaJugadores = emptyList()
            onDispose { }
        } else {
            cargandoJugadores = true
            val registro: ListenerRegistration = db.collection("jugadores_globales")
                .whereEqualTo("equipoId", equipoId)
                .addSnapshotListener { snapshot, error ->
                    cargandoJugadores = false
                    if (error != null) {
                        Log.e("LECTURA", "jugadores_globales: ${error.message}", error)
                        Toast.makeText(
                            contexto,
                            "No se pudo cargar la nómina: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }
                    listaJugadores = snapshot?.toObjects(Jugador::class.java).orEmpty()
                        .filter { it.id.isNotBlank() && it.activo }
                        .sortedBy { it.dorsal }
                }
            onDispose { registro.remove() }
        }
    }

    /** Baja logica: el documento se conserva, solo se apaga el campo `activo`. */
    fun darDeBaja(jugador: Jugador) {
        db.collection("jugadores_globales").document(jugador.id)
            .update("activo", false)
            .addOnSuccessListener {
                Toast.makeText(contexto, "${jugador.nombre} dado de baja", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("GUARDADO", "jugadores_globales/${jugador.id} baja fallida", e)
                Toast.makeText(
                    contexto,
                    "No se pudo dar de baja: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera Elegante
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorTarjeta),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = colorAcento, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("GESTIÓN DE PLANTILLAS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Administra tu alineación oficial", color = colorTextoSecundario, fontSize = 12.sp)
                }
            }
        }

        // Selector de Equipo Estilizado
        ExposedDropdownMenuBox(
            expanded = expandedDropdown,
            onExpandedChange = { expandedDropdown = !expandedDropdown }
        ) {
            OutlinedTextField(
                value = equipoSeleccionado?.nombre ?: "Sin equipos registrados",
                onValueChange = {},
                readOnly = true,
                label = { Text("Club / Equipo Seleccionado") },
                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = colorAcento) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = colorTarjeta,
                    unfocusedContainerColor = colorTarjeta,
                    focusedBorderColor = colorAcento,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = colorAcento,
                    unfocusedLabelColor = Color.Gray,
                    focusedTrailingIconColor = colorAcento,
                    unfocusedTrailingIconColor = Color.Gray
                )
            )
            ExposedDropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier.background(colorTarjeta)
            ) {
                listaEquipos.forEach { equipo ->
                    DropdownMenuItem(
                        text = { Text(equipo.nombre, color = Color.White) },
                        onClick = {
                            equipoSeleccionado = equipo
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        // Título de la lista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jugadores Inscritos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(
                color = colorAcento.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${listaJugadores.size} Activos",
                    color = colorAcento,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        when {
            cargandoJugadores -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorAcento)
                }
            }

            listaJugadores.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (equipoSeleccionado == null) {
                            "Registra primero un equipo en el panel de administración."
                        } else {
                            "Este equipo todavía no tiene jugadores inscritos."
                        },
                        color = colorTextoSecundario,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            else -> {
                // Lista de Jugadores Animada
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(listaJugadores, key = { it.id }) { jugador ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Círculo de Dorsal Estilizado
                                    Surface(
                                        shape = CircleShape,
                                        color = colorFondo,
                                        border = BorderStroke(1.dp, colorAcento.copy(alpha = 0.5f)),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = jugador.dorsal.toString(),
                                                color = colorAcento,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(text = jugador.nombre, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // El modelo Jugador no guarda posicion; se muestra
                                        // lo que si existe en el documento.
                                        Text(
                                            text = buildString {
                                                if (jugador.cedula.isNotBlank()) append("CI ${jugador.cedula}")
                                                if (jugador.edad > 0) {
                                                    if (isNotEmpty()) append(" · ")
                                                    append("${jugador.edad} años")
                                                }
                                                if (isEmpty()) append("Sin datos adicionales")
                                            },
                                            color = colorTextoSecundario,
                                            fontSize = 12.sp
                                        )
                                        if (jugador.estaSuspendido) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("🟥 Suspendido", color = colorRojo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Botón de Dar de Baja
                                Surface(
                                    onClick = {
                                        jugadorParaBaja = jugador
                                        mostrarDialogoBaja = true
                                    },
                                    shape = CircleShape,
                                    color = colorRojo.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PersonRemove, contentDescription = "Dar de baja", tint = colorRojo, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cuadro de Diálogo Premium para Dar de Baja
    val enBaja = jugadorParaBaja
    if (mostrarDialogoBaja && enBaja != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBaja = false },
            containerColor = colorTarjeta, // Fondo oscuro para el diálogo
            titleContentColor = Color.White,
            textContentColor = colorTextoSecundario,
            title = {
                Text("Confirmar Baja", fontWeight = FontWeight.Black)
            },
            text = {
                Text(
                    "¿Retirar a ${enBaja.nombre} de la plantilla de " +
                        "${equipoSeleccionado?.nombre ?: "este equipo"}? " +
                        "Deja de aparecer en la nómina, pero se conserva su historial de goles."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        darDeBaja(enBaja)
                        mostrarDialogoBaja = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorRojo)
                ) {
                    Text("Dar de baja", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoBaja = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
