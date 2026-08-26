package com.sofia.multisport.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Transaction
import com.sofia.multisport.data.models.FilaGoleador
import com.sofia.multisport.data.models.FilaPosicion
import com.sofia.multisport.data.models.Jugador
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

/*
 * Panel de arbitraje en vivo.
 */

// =============================================================================
// 1. PANTALLA
// =============================================================================

private object Paleta {
    val fondo = Color(0xFF14081E)
    val barra = Color(0xFF26004C)
    val tarjeta = Color(0xFF311547)
    val boton = Color(0xFF452B5B)
    val verde = Color(0xFF4CAF50)
    val amarillo = Color(0xFFFFC107)
    val rojo = Color(0xFFE53935)
    val texto = Color(0xFFA191B0)
    val cancha = Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)))
}

private const val TAB_LOCAL = 0
private const val TAB_TIMELINE = 2

@Composable
fun ArbitrajePantalla(
    partidoId: String,
    onBack: () -> Unit,
    viewModel: ArbitrajeViewModel = viewModel(
        key = "arbitraje_$partidoId",
        factory = ArbitrajeViewModel.factoria(partidoId)
    )
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Paleta.fondo,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { BarraSuperior(estado, onBack) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val fallo = estado.errorFatal
            when {
                fallo != null -> MensajeCentral(fallo, Paleta.rojo)
                estado.cargando -> CircularProgressIndicator(
                    color = Paleta.amarillo,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> ContenidoArbitraje(estado, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperior(estado: ArbitrajeUiState, onBack: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Paleta.barra,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                // CORRECCIÓN: Uso de ArrowBack estándar compatible
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
        },
        title = { Text("PANEL DE ÁRBITRO", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        actions = {
            val (etiqueta, color) = when (estado.estado) {
                EstadoPartido.EN_CURSO -> "EN VIVO" to Paleta.verde
                EstadoPartido.TERMINADO -> "FINALIZADO" to Paleta.rojo
                EstadoPartido.PROGRAMADO -> "PROGRAMADO" to Paleta.boton
            }
            Surface(
                color = color,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    etiqueta,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    )
}

@Composable
private fun MensajeCentral(texto: String, color: Color) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = color, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContenidoArbitraje(estado: ArbitrajeUiState, vm: ArbitrajeViewModel) {
    var tabSeleccionado by rememberSaveable { mutableIntStateOf(TAB_LOCAL) }
    var mostrarFinalizar by rememberSaveable { mutableStateOf(false) }
    var mostrarCambio by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        TarjetaPartido(estado, vm)
        Spacer(Modifier.height(12.dp))
        BarraAcciones(estado, vm) { mostrarFinalizar = true }
        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = tabSeleccionado,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { posiciones ->
                if (tabSeleccionado < posiciones.size) {
                    Box(
                        Modifier
                            .tabIndicatorOffset(posiciones[tabSeleccionado])
                            .height(3.dp)
                            .background(Paleta.amarillo)
                    )
                }
            }
        ) {
            listOf("Local", "Visitante", "Eventos").forEachIndexed { indice, titulo ->
                val activo = tabSeleccionado == indice
                Tab(
                    selected = activo,
                    onClick = { tabSeleccionado = indice },
                    text = {
                        Text(
                            titulo,
                            fontSize = 13.sp,
                            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                            color = if (activo) Color.White else Paleta.texto
                        )
                    }
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .padding(top = 10.dp)
        ) {
            if (tabSeleccionado == TAB_TIMELINE) {
                ListaEventos(estado.timeline)
            } else {
                ListaJugadores(
                    estado = estado,
                    esLocal = tabSeleccionado == TAB_LOCAL,
                    vm = vm,
                    onSustitucion = { mostrarCambio = true }
                )
            }
        }
    }

    if (mostrarFinalizar) {
        DialogoFinalizar(
            onConfirmar = { vm.finalizarPartido(); mostrarFinalizar = false },
            onCancelar = { mostrarFinalizar = false }
        )
    }

    if (mostrarCambio) {
        DialogoSustitucion(
            jugadores = estado.jugadoresDe(tabSeleccionado == TAB_LOCAL).filterNot { it.estaSuspendido },
            onConfirmar = { sale, entra -> vm.registrarCambio(sale, entra); mostrarCambio = false },
            onCancelar = { mostrarCambio = false }
        )
    }
}

// ------------------------------------------------------------------- marcador

@Composable
private fun TarjetaPartido(estado: ArbitrajeUiState, vm: ArbitrajeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Paleta.tarjeta)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Partido en vivo",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (estado.estado == EstadoPartido.EN_CURSO) "Min ${estado.minutoActual}'"
                    else estado.estado.valorFirestore,
                    color = Paleta.amarillo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Paleta.cancha),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NombreEquipo(estado.nombreLocal, Modifier.weight(1f))
                        Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "${estado.marcadorLocal} - ${estado.marcadorVisitante}",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                            )
                        }
                        NombreEquipo(estado.nombreVisitante, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColumnaEstadisticas(estado.statsLocal)
                            Text(
                                "ESTADÍSTICAS",
                                color = Paleta.texto,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            ColumnaEstadisticas(estado.statsVisitante)
                        }
                    }
                }

                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        estado.eventoReciente,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            BarraCronometro(estado, vm)
        }
    }
}

@Composable
private fun NombreEquipo(nombre: String, modifier: Modifier) {
    Text(
        nombre,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ColumnaEstadisticas(stats: EstadisticasEquipo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "🟨 ${stats.amarillas} | 🟥 ${stats.rojas}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text("🚩 ${stats.corners} corners", color = Paleta.amarillo, fontSize = 10.sp)
    }
}

@Composable
private fun BarraCronometro(estado: ArbitrajeUiState, vm: ArbitrajeViewModel) {
    val segundos = estado.segundosTranscurridos
    val reloj = remember(segundos) {
        "%02d:%02d".format(Locale.US, segundos / 60L, segundos % 60L)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(reloj, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (estado.cronometro.tiempoAnadido > 0) {
                Spacer(Modifier.width(6.dp))
                Surface(color = Paleta.amarillo, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        "+${estado.cronometro.tiempoAnadido}'",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (!estado.partidoCerrado) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonCompacto("Corner L", estado.puedeRegistrarEventos) { vm.registrarCorner(true) }
                BotonCompacto("Corner V", estado.puedeRegistrarEventos) { vm.registrarCorner(false) }

                Surface(
                    onClick = vm::alternarCronometro,
                    shape = RoundedCornerShape(6.dp),
                    color = Paleta.verde,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (estado.cronometro.corriendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (estado.cronometro.corriendo) "Pausar" else "Iniciar",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Surface(
                    onClick = vm::cerrarPeriodo,
                    enabled = estado.estado == EstadoPartido.EN_CURSO,
                    shape = RoundedCornerShape(6.dp),
                    color = Paleta.rojo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Fin de periodo",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                BotonCompacto("−1'", estado.cronometro.tiempoAnadido > 0) { vm.ajustarTiempoAnadido(-1) }
                BotonCompacto("+1'", true) { vm.ajustarTiempoAnadido(1) }
            }
        }
    }
}

@Composable
private fun BotonCompacto(texto: String, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        color = Paleta.boton,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.alpha(if (habilitado) 1f else 0.4f)
    ) {
        Text(
            texto,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
        )
    }
}

// ------------------------------------------------------------------- acciones

@Composable
private fun BarraAcciones(estado: ArbitrajeUiState, vm: ArbitrajeViewModel, onFinalizar: () -> Unit) {
    var nota by rememberSaveable { mutableStateOf("") }
    val enviar = {
        if (nota.isNotBlank()) {
            vm.enviarAviso(nota)
            nota = ""
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(Paleta.tarjeta, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = enviar,
            enabled = nota.isNotBlank(),
            color = Paleta.verde,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .size(36.dp)
                .alpha(if (nota.isNotBlank()) 1f else 0.4f)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Enviar aviso",
                tint = Color.White,
                modifier = Modifier.padding(6.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        TextField(
            value = nota,
            onValueChange = { nota = it },
            placeholder = { Text("Aviso o URL de transmisión...", color = Paleta.texto, fontSize = 13.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Paleta.amarillo,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { enviar() }),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            singleLine = true
        )
        if (!estado.partidoCerrado) {
            Surface(color = Paleta.rojo, shape = RoundedCornerShape(6.dp), onClick = onFinalizar) {
                Text(
                    "FINALIZAR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------- listados

@Composable
private fun ListaEventos(eventos: List<EventoPartido>) {
    if (eventos.isEmpty()) {
        MensajeCentral("Todavía no hay eventos registrados", Paleta.texto)
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        items(
            items = eventos.asReversed(),
            key = { "${it.timestamp}_${it.tipo}_${it.jugadorId}" }
        ) { evento ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Paleta.tarjeta),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(evento.icono, fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            evento.descripcion,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text("Minuto ${evento.minuto}'", color = Paleta.amarillo, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaJugadores(
    estado: ArbitrajeUiState,
    esLocal: Boolean,
    vm: ArbitrajeViewModel,
    onSustitucion: () -> Unit
) {
    val jugadores = estado.jugadoresDe(esLocal)

    Column {
        if (estado.puedeRegistrarEventos) {
            Surface(
                onClick = onSustitucion,
                enabled = jugadores.size >= 2,
                color = Paleta.boton,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    "🔄 Realizar sustitución",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
            }
        }

        if (jugadores.isEmpty()) {
            MensajeCentral("Sin jugadores registrados en este equipo", Paleta.texto)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(items = jugadores, key = { it.id }) { jugador ->
                TarjetaJugador(
                    jugador = jugador,
                    editable = estado.puedeRegistrarEventos,
                    onGol = { vm.registrarGol(jugador) },
                    onAmarilla = { vm.registrarTarjeta(jugador, esAmarilla = true) },
                    onRoja = { vm.registrarTarjeta(jugador, esAmarilla = false) },
                    onAnular = { vm.anularGol(jugador) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaJugador(
    jugador: Jugador,
    editable: Boolean,
    onGol: () -> Unit,
    onAmarilla: () -> Unit,
    onRoja: () -> Unit,
    onAnular: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Paleta.tarjeta, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .alpha(if (jugador.estaSuspendido) 0.5f else 1f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Text(
                "${jugador.dorsal}. ${jugador.nombre}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (jugador.estaSuspendido) {
                Surface(color = Paleta.rojo, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        "EXPULSADO",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (editable && !jugador.estaSuspendido) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AccionJugador("Gol", "⚽", Modifier.weight(1f), onGol)
                AccionJugador("Ama", "🟨", Modifier.weight(1f), onAmarilla)
                AccionJugador("Roj", "🟥", Modifier.weight(1f), onRoja)
                AccionJugador("Anu", "❌", Modifier.weight(1f), onAnular)
            }
        }
    }
}

@Composable
private fun AccionJugador(etiqueta: String, icono: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Paleta.boton,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            Text(etiqueta, color = Paleta.texto, fontSize = 11.sp)
            Text(icono, color = Color.White)
        }
    }
}

// ------------------------------------------------------------------- diálogos

@Composable
private fun DialogoFinalizar(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Paleta.tarjeta,
        titleContentColor = Color.White,
        textContentColor = Paleta.texto,
        shape = RoundedCornerShape(12.dp),
        title = { Text("Finalizar partido", fontWeight = FontWeight.Bold) },
        text = { Text("El partido se cerrará y se sumarán los puntos a la tabla. Acción irreversible.") },
        confirmButton = {
            Button(onClick = onConfirmar, colors = ButtonDefaults.buttonColors(containerColor = Paleta.rojo)) {
                Text("Cerrar partido", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar, border = BorderStroke(1.dp, Paleta.texto)) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}

@Composable
private fun DialogoSustitucion(
    jugadores: List<Jugador>,
    onConfirmar: (sale: Jugador, entra: Jugador) -> Unit,
    onCancelar: () -> Unit
) {
    var sale by remember { mutableStateOf<Jugador?>(null) }
    var entra by remember { mutableStateOf<Jugador?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Paleta.tarjeta,
        titleContentColor = Color.White,
        textContentColor = Paleta.texto,
        shape = RoundedCornerShape(12.dp),
        title = { Text("Sustitución", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Text("Jugador que sale 🔻", color = Paleta.rojo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                SelectorJugador(
                    jugadores = jugadores,
                    seleccionado = sale,
                    color = Paleta.rojo,
                    modifier = Modifier.weight(1f),
                    onSeleccionar = {
                        sale = it
                        if (entra?.id == it.id) entra = null
                    }
                )
                Spacer(Modifier.height(6.dp))
                Text("Jugador que entra 🔼", color = Paleta.verde, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                SelectorJugador(
                    jugadores = jugadores.filter { it.id != sale?.id },
                    seleccionado = entra,
                    color = Paleta.verde,
                    modifier = Modifier.weight(1f),
                    onSeleccionar = { entra = it }
                )
            }
        },
        confirmButton = {
            val salienteActual = sale
            val entranteActual = entra
            Button(
                onClick = {
                    if (salienteActual != null && entranteActual != null) {
                        onConfirmar(salienteActual, entranteActual)
                    }
                },
                enabled = salienteActual != null && entranteActual != null,
                colors = ButtonDefaults.buttonColors(containerColor = Paleta.verde)
            ) {
                Text("Realizar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar, border = BorderStroke(1.dp, Paleta.texto)) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}

@Composable
private fun SelectorJugador(
    jugadores: List<Jugador>,
    seleccionado: Jugador?,
    color: Color,
    modifier: Modifier,
    onSeleccionar: (Jugador) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(Paleta.fondo, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        items(items = jugadores, key = { it.id }) { jugador ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = seleccionado?.id == jugador.id,
                    onClick = { onSeleccionar(jugador) },
                    colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = Color.Gray)
                )
                Text("${jugador.dorsal}. ${jugador.nombre}", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// =============================================================================
// 2. VIEWMODEL
// =============================================================================

class ArbitrajeViewModel(
    private val partidoId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val partidoRef: DocumentReference = db.collection(Campos.COL_PARTIDOS).document(partidoId)

    private val _estado = MutableStateFlow(ArbitrajeUiState())
    val estado: StateFlow<ArbitrajeUiState> = _estado.asStateFlow()

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    private var listenerPartido: ListenerRegistration? = null
    private var listenerJugadores: ListenerRegistration? = null
    private var jobCronometro: Job? = null

    private var equiposObservados: Pair<String, String>? = null
    private var minutoPublicado: Int = -1

    init {
        require(partidoId.isNotBlank()) { "partidoId vacio" }
        observarPartido()
    }

    override fun onCleared() {
        listenerPartido?.remove()
        listenerJugadores?.remove()
        jobCronometro?.cancel()
        _mensajes.close()
    }

    private fun observarPartido() {
        listenerPartido = partidoRef.addSnapshotListener { snapshot, error ->
            when {
                error != null -> _estado.update {
                    it.copy(cargando = false, errorFatal = "Sin conexion con el partido: ${error.message}")
                }

                snapshot == null || !snapshot.exists() -> _estado.update {
                    it.copy(cargando = false, errorFatal = "El partido ya no existe")
                }

                else -> aplicarSnapshot(snapshot)
            }
        }
    }

    private fun aplicarSnapshot(snapshot: DocumentSnapshot) {
        val cronometro = Cronometro(
            corriendo = snapshot.getBoolean(Campos.CRONO_CORRIENDO) ?: false,
            segundosAcumulados = snapshot.getLong(Campos.CRONO_SEGUNDOS)
                ?: ((snapshot.getLong(Campos.MINUTO_ACTUAL) ?: 0L) * 60L),
            inicioEpochMillis = snapshot.getTimestamp(Campos.CRONO_INICIO)?.toDate()?.time,
            tiempoAnadido = (snapshot.getLong(Campos.TIEMPO_ANADIDO) ?: 0L).toInt()
        )

        val nuevo = _estado.value.copy(
            cargando = false,
            errorFatal = null,
            marcadorLocal = snapshot.entero(Campos.PUNTUACION_LOCAL),
            marcadorVisitante = snapshot.entero(Campos.PUNTUACION_VISITANTE),
            nombreLocal = snapshot.getString(Campos.NOMBRE_LOCAL) ?: "Local",
            nombreVisitante = snapshot.getString(Campos.NOMBRE_VISITANTE) ?: "Visitante",
            equipoLocalId = snapshot.getString(Campos.EQUIPO_LOCAL_ID).orEmpty(),
            equipoVisitanteId = snapshot.getString(Campos.EQUIPO_VISITANTE_ID).orEmpty(),
            estado = EstadoPartido.desde(snapshot.getString(Campos.ESTADO)),
            eventoReciente = snapshot.getString(Campos.EVENTO_RECIENTE) ?: "Esperando inicio...",
            deporteId = snapshot.getString(Campos.DEPORTE_ID) ?: "futbol",
            campeonatoId = snapshot.getString(Campos.CAMPEONATO_ID).orEmpty(),
            statsLocal = EstadisticasEquipo(
                amarillas = snapshot.entero(Campos.AMARILLAS_LOCAL),
                rojas = snapshot.entero(Campos.ROJAS_LOCAL),
                corners = snapshot.entero(Campos.CORNERS_LOCAL)
            ),
            statsVisitante = EstadisticasEquipo(
                amarillas = snapshot.entero(Campos.AMARILLAS_VISITANTE),
                rojas = snapshot.entero(Campos.ROJAS_VISITANTE),
                corners = snapshot.entero(Campos.CORNERS_VISITANTE)
            ),
            cronometro = cronometro,
            segundosTranscurridos = cronometro.segundosEn(ahora()),
            timeline = EventoPartido.leerDe(snapshot),
            resultadoAplicado = snapshot.getBoolean(Campos.RESULTADO_APLICADO) ?: false
        )

        _estado.value = nuevo
        observarJugadores(nuevo.equipoLocalId, nuevo.equipoVisitanteId)
        sincronizarCronometro(cronometro)
    }

    private fun observarJugadores(localId: String, visitanteId: String) {
        val ids = listOf(localId, visitanteId).filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return
        val clave = localId to visitanteId
        if (equiposObservados == clave) return
        equiposObservados = clave

        listenerJugadores?.remove()
        listenerJugadores = db.collection(Campos.COL_JUGADORES)
            .whereIn("equipoId", ids)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    avisar("No se pudo cargar la nomina: ${error.message}")
                    return@addSnapshotListener
                }
                val jugadores = snapshot?.toObjects(Jugador::class.java)
                    ?.filter { it.id.isNotBlank() }
                    ?.sortedBy { it.dorsal }
                    .orEmpty()
                _estado.update {
                    it.copy(
                        jugadoresLocal = jugadores.filter { j -> j.equipoId == localId },
                        jugadoresVisitante = jugadores.filter { j -> j.equipoId == visitanteId }
                    )
                }
            }
    }

    private fun sincronizarCronometro(cronometro: Cronometro) {
        jobCronometro?.cancel()
        if (!cronometro.corriendo) return

        jobCronometro = viewModelScope.launch {
            while (isActive) {
                val segundos = cronometro.segundosEn(ahora())
                _estado.update { it.copy(segundosTranscurridos = segundos) }
                val minuto = (segundos / 60L).toInt()
                if (minuto != minutoPublicado) {
                    minutoPublicado = minuto
                    partidoRef.update(Campos.MINUTO_ACTUAL, minuto)
                }
                delay(500L)
            }
        }
    }

    fun alternarCronometro() {
        val s = _estado.value
        if (s.partidoCerrado) return avisar("El partido ya esta cerrado")
        val crono = s.cronometro

        val cambios = mutableMapOf<String, Any>()
        if (crono.corriendo) {
            cambios[Campos.CRONO_SEGUNDOS] = crono.segundosEn(ahora())
            cambios[Campos.CRONO_INICIO] = FieldValue.delete()
            cambios[Campos.CRONO_CORRIENDO] = false
        } else {
            cambios[Campos.CRONO_SEGUNDOS] = crono.segundosAcumulados
            cambios[Campos.CRONO_INICIO] = Timestamp(Date(ahora()))
            cambios[Campos.CRONO_CORRIENDO] = true
        }
        if (s.estado == EstadoPartido.PROGRAMADO) {
            cambios[Campos.ESTADO] = EstadoPartido.EN_CURSO.valorFirestore
            val evento = nuevoEvento(s, TipoEvento.INICIO, "▶️ Comienza el partido")
            cambios[Campos.TIMELINE] = FieldValue.arrayUnion(evento)
            cambios[Campos.EVENTO_RECIENTE] = evento["descripcion"] as String
        }
        aplicar(cambios)
    }

    fun cerrarPeriodo() {
        val s = _estado.value
        if (s.partidoCerrado || s.estado == EstadoPartido.PROGRAMADO) return
        val evento = nuevoEvento(s, TipoEvento.FIN_PERIODO, "⏸️ Fin del periodo")
        aplicar(
            mapOf(
                Campos.CRONO_SEGUNDOS to s.cronometro.segundosEn(ahora()),
                Campos.CRONO_INICIO to FieldValue.delete(),
                Campos.CRONO_CORRIENDO to false,
                Campos.TIMELINE to FieldValue.arrayUnion(evento),
                Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
            )
        )
    }

    fun ajustarTiempoAnadido(delta: Int) {
        val s = _estado.value
        if (s.partidoCerrado) return
        val nuevo = (s.cronometro.tiempoAnadido + delta).coerceIn(0, 15)
        if (nuevo == s.cronometro.tiempoAnadido) return
        aplicar(mapOf(Campos.TIEMPO_ANADIDO to nuevo))
    }

    fun registrarCorner(esLocal: Boolean) {
        val s = _estado.value
        if (!exigirEnCurso(s)) return
        val campo = if (esLocal) Campos.CORNERS_LOCAL else Campos.CORNERS_VISITANTE
        val equipo = if (esLocal) s.nombreLocal else s.nombreVisitante
        val evento = nuevoEvento(s, TipoEvento.CORNER, "🚩 Tiro de esquina para $equipo")
        aplicar(
            mapOf(
                campo to FieldValue.increment(1),
                Campos.TIMELINE to FieldValue.arrayUnion(evento),
                Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
            )
        )
    }

    fun registrarGol(jugador: Jugador) {
        val s = _estado.value
        if (!exigirEnCurso(s) || !exigirJugadorValido(jugador)) return

        val esLocal = s.esEquipoLocal(jugador.equipoId)
        val campo = if (esLocal) Campos.PUNTUACION_LOCAL else Campos.PUNTUACION_VISITANTE
        val equipo = if (esLocal) s.nombreLocal else s.nombreVisitante
        val evento = nuevoEvento(
            s, TipoEvento.GOL,
            "⚽ ¡GOL DE $equipo! (${jugador.nombre})", jugador.id
        )
        val goleadorRef = goleadorRef(s.campeonatoId, jugador.id)

        db.runTransaction { t ->
            val snapGoleador = goleadorRef?.let { t.get(it) }
            t.update(
                partidoRef,
                mapOf(
                    campo to FieldValue.increment(1),
                    Campos.TIMELINE to FieldValue.arrayUnion(evento),
                    Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
                )
            )
            if (goleadorRef != null) {
                if (snapGoleador?.exists() == true) {
                    t.update(goleadorRef, Campos.GOLES, FieldValue.increment(1))
                } else {
                    t.set(
                        goleadorRef,
                        FilaGoleador(
                            jugadorId = jugador.id,
                            jugadorNombre = jugador.nombre,
                            equipoId = jugador.equipoId,
                            equipoNombre = equipo,
                            deporteId = s.deporteId,
                            campeonatoId = s.campeonatoId,
                            goles = 1
                        )
                    )
                }
            }
        }.addOnFailureListener { avisar("No se registro el gol: ${it.message}") }
    }

    fun anularGol(jugador: Jugador) {
        val s = _estado.value
        if (!exigirEnCurso(s) || !exigirJugadorValido(jugador)) return

        val esLocal = s.esEquipoLocal(jugador.equipoId)
        val campo = if (esLocal) Campos.PUNTUACION_LOCAL else Campos.PUNTUACION_VISITANTE
        val evento = nuevoEvento(
            s, TipoEvento.GOL_ANULADO,
            "❌ GOL ANULADO: ${jugador.nombre}", jugador.id
        )
        val goleadorRef = goleadorRef(s.campeonatoId, jugador.id)

        db.runTransaction { t ->
            val snapPartido = t.get(partidoRef)
            val timeline = EventoPartido.leerDe(snapPartido)
            val vigentes = timeline.count { it.tipo == TipoEvento.GOL.name && it.jugadorId == jugador.id } -
                    timeline.count { it.tipo == TipoEvento.GOL_ANULADO.name && it.jugadorId == jugador.id }
            if (vigentes <= 0) return@runTransaction false
            if ((snapPartido.getLong(campo) ?: 0L) <= 0L) return@runTransaction false

            val snapGoleador = goleadorRef?.let { t.get(it) }

            t.update(
                partidoRef,
                mapOf(
                    campo to FieldValue.increment(-1),
                    Campos.TIMELINE to FieldValue.arrayUnion(evento),
                    Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
                )
            )
            if (goleadorRef != null && snapGoleador?.exists() == true) {
                val goles = snapGoleador.getLong(Campos.GOLES) ?: 0L
                if (goles > 1L) t.update(goleadorRef, Campos.GOLES, FieldValue.increment(-1))
                else t.delete(goleadorRef)
            }
            true
        }.addOnSuccessListener { anulado ->
            if (anulado != true) avisar("${jugador.nombre} no tiene goles vigentes en este partido")
        }.addOnFailureListener { avisar("No se anulo el gol: ${it.message}") }
    }

    fun registrarTarjeta(jugador: Jugador, esAmarilla: Boolean) {
        val s = _estado.value
        if (!exigirEnCurso(s) || !exigirJugadorValido(jugador)) return

        val esLocal = s.esEquipoLocal(jugador.equipoId)
        val campoAmarillas = if (esLocal) Campos.AMARILLAS_LOCAL else Campos.AMARILLAS_VISITANTE
        val campoRojas = if (esLocal) Campos.ROJAS_LOCAL else Campos.ROJAS_VISITANTE
        val jugadorRef = db.collection(Campos.COL_JUGADORES).document(jugador.id)
        val minuto = s.minutoActual

        db.runTransaction { t ->
            val snapPartido = t.get(partidoRef)
            val amarillasPrevias = EventoPartido.leerDe(snapPartido)
                .count { it.tipo == TipoEvento.TARJETA_AMARILLA.name && it.jugadorId == jugador.id }

            val expulsaPorDoble = esAmarilla && amarillasPrevias >= 1
            val cambios = mutableMapOf<String, Any>()
            val eventos = mutableListOf<Map<String, Any>>()

            if (esAmarilla) {
                cambios[campoAmarillas] = FieldValue.increment(1)
                eventos += evento(
                    minuto, TipoEvento.TARJETA_AMARILLA,
                    "🟨 Amarilla: ${jugador.nombre}", jugador.id
                )
            }
            if (!esAmarilla || expulsaPorDoble) {
                val motivo = if (expulsaPorDoble) "Doble amarilla (expulsion)" else "Roja directa"
                cambios[campoRojas] = FieldValue.increment(1)
                eventos += evento(
                    minuto, TipoEvento.TARJETA_ROJA,
                    "🟥 $motivo: ${jugador.nombre}", jugador.id
                )
                t.update(jugadorRef, Campos.JUGADOR_SUSPENDIDO, true)
            }

            cambios[Campos.TIMELINE] = FieldValue.arrayUnion(*eventos.toTypedArray())
            cambios[Campos.EVENTO_RECIENTE] = eventos.last()["descripcion"] as String
            t.update(partidoRef, cambios)
            expulsaPorDoble
        }.addOnSuccessListener { dobleAmarilla ->
            if (dobleAmarilla == true) avisar("EXPULSION: doble amarilla de ${jugador.nombre}")
        }.addOnFailureListener { avisar("No se registro la tarjeta: ${it.message}") }
    }

    fun registrarCambio(sale: Jugador, entra: Jugador) {
        val s = _estado.value
        if (!exigirEnCurso(s)) return
        if (sale.id == entra.id) return avisar("El jugador que entra debe ser distinto del que sale")
        if (sale.equipoId != entra.equipoId) return avisar("Ambos jugadores deben ser del mismo equipo")

        val evento = nuevoEvento(
            s, TipoEvento.CAMBIO,
            "🔄 Cambio: entra ${entra.nombre} | sale ${sale.nombre}", entra.id
        )
        aplicar(
            mapOf(
                Campos.TIMELINE to FieldValue.arrayUnion(evento),
                Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
            )
        )
    }

    fun enviarAviso(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        val s = _estado.value

        if (limpio.startsWith("http://") || limpio.startsWith("https://")) {
            val evento = nuevoEvento(s, TipoEvento.AVISO, "📢 Link de transmision actualizado")
            aplicar(
                mapOf(
                    Campos.LINK_TRANSMISION to limpio,
                    Campos.TIMELINE to FieldValue.arrayUnion(evento),
                    Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
                ),
                exito = "Transmision actualizada"
            )
        } else {
            val evento = nuevoEvento(s, TipoEvento.AVISO, "📢 $limpio")
            aplicar(
                mapOf(
                    Campos.TIMELINE to FieldValue.arrayUnion(evento),
                    Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
                )
            )
        }
    }

    fun finalizarPartido() {
        val s = _estado.value
        if (s.partidoCerrado) return avisar("El partido ya estaba cerrado")

        val liquidaTabla = s.campeonatoId.isNotBlank() &&
                s.equipoLocalId.isNotBlank() &&
                s.equipoVisitanteId.isNotBlank() &&
                s.equipoLocalId != s.equipoVisitanteId

        val refLocal = if (liquidaTabla) posicionRef(s.campeonatoId, s.equipoLocalId) else null
        val refVisitante = if (liquidaTabla) posicionRef(s.campeonatoId, s.equipoVisitanteId) else null
        val evento = nuevoEvento(s, TipoEvento.FIN_PARTIDO, "⏹️ Partido finalizado")

        db.runTransaction { t ->
            val snapPartido = t.get(partidoRef)
            if (snapPartido.getBoolean(Campos.RESULTADO_APLICADO) == true) return@runTransaction false

            val golesLocal = snapPartido.entero(Campos.PUNTUACION_LOCAL)
            val golesVisitante = snapPartido.entero(Campos.PUNTUACION_VISITANTE)
            val snapLocal = refLocal?.let { t.get(it) }
            val snapVisitante = refVisitante?.let { t.get(it) }

            t.update(
                partidoRef,
                mapOf(
                    Campos.ESTADO to EstadoPartido.TERMINADO.valorFirestore,
                    Campos.CRONO_CORRIENDO to false,
                    Campos.CRONO_SEGUNDOS to s.cronometro.segundosEn(ahora()),
                    Campos.CRONO_INICIO to FieldValue.delete(),
                    Campos.RESULTADO_APLICADO to true,
                    Campos.TIMELINE to FieldValue.arrayUnion(evento),
                    Campos.EVENTO_RECIENTE to (evento["descripcion"] as String)
                )
            )

            if (refLocal != null && snapLocal != null) {
                liquidar(t, refLocal, snapLocal, s, s.equipoLocalId, s.nombreLocal, golesLocal, golesVisitante)
            }
            if (refVisitante != null && snapVisitante != null) {
                liquidar(
                    t, refVisitante, snapVisitante, s,
                    s.equipoVisitanteId, s.nombreVisitante, golesVisitante, golesLocal
                )
            }
            true
        }.addOnSuccessListener { aplicado ->
            avisar(if (aplicado == true) "Partido cerrado y tabla actualizada" else "El resultado ya estaba aplicado")
        }.addOnFailureListener { avisar("No se pudo cerrar el partido: ${it.message}") }
    }

    private fun liquidar(
        t: Transaction,
        ref: DocumentReference,
        snap: DocumentSnapshot,
        s: ArbitrajeUiState,
        equipoId: String,
        equipoNombre: String,
        golesFavor: Int,
        golesContra: Int
    ) {
        val puntos = when {
            golesFavor > golesContra -> 3
            golesFavor == golesContra -> 1
            else -> 0
        }
        val letra = when (puntos) {
            3 -> "G"
            1 -> "E"
            else -> "P"
        }

        if (!snap.exists()) {
            t.set(
                ref,
                FilaPosicion(
                    equipoId = equipoId,
                    equipoNombre = equipoNombre,
                    deporteId = s.deporteId,
                    campeonatoId = s.campeonatoId,
                    partidosJugados = 1,
                    partidosGanados = if (puntos == 3) 1 else 0,
                    partidosEmpatados = if (puntos == 1) 1 else 0,
                    partidosPerdidos = if (puntos == 0) 1 else 0,
                    golesFavor = golesFavor,
                    golesContra = golesContra,
                    golDiferencia = golesFavor - golesContra,
                    puntos = puntos,
                    historial = listOf(letra)
                )
            )
            return
        }

        val gf = snap.entero("golesFavor") + golesFavor
        val gc = snap.entero("golesContra") + golesContra
        @Suppress("UNCHECKED_CAST")
        val historial = (snap.get("historial") as? List<String>).orEmpty()

        t.update(
            ref,
            mapOf(
                "equipoNombre" to equipoNombre,
                "deporteId" to s.deporteId,
                "campeonatoId" to s.campeonatoId,
                "partidosJugados" to snap.entero("partidosJugados") + 1,
                "partidosGanados" to snap.entero("partidosGanados") + if (puntos == 3) 1 else 0,
                "partidosEmpatados" to snap.entero("partidosEmpatados") + if (puntos == 1) 1 else 0,
                "partidosPerdidos" to snap.entero("partidosPerdidos") + if (puntos == 0) 1 else 0,
                "golesFavor" to gf,
                "golesContra" to gc,
                "golDiferencia" to gf - gc,
                "puntos" to snap.entero("puntos") + puntos,
                "historial" to (listOf(letra) + historial).take(5)
            )
        )
    }

    private fun aplicar(cambios: Map<String, Any>, exito: String? = null) {
        partidoRef.update(cambios)
            .addOnSuccessListener { exito?.let(::avisar) }
            .addOnFailureListener { avisar("No se pudo guardar: ${it.message}") }
    }

    private fun exigirEnCurso(s: ArbitrajeUiState): Boolean {
        if (s.puedeRegistrarEventos) return true
        avisar(
            if (s.partidoCerrado) "El partido ya esta cerrado"
            else "Inicia el partido antes de registrar eventos"
        )
        return false
    }

    private fun exigirJugadorValido(jugador: Jugador): Boolean {
        if (jugador.id.isNotBlank()) return true
        avisar("El jugador no tiene identificador valido")
        return false
    }

    private fun nuevoEvento(
        s: ArbitrajeUiState,
        tipo: TipoEvento,
        descripcion: String,
        jugadorId: String = ""
    ): Map<String, Any> = evento(s.minutoActual, tipo, descripcion, jugadorId)

    private fun evento(
        minuto: Int,
        tipo: TipoEvento,
        descripcion: String,
        jugadorId: String = ""
    ): Map<String, Any> = mapOf(
        "minuto" to minuto,
        "tipo" to tipo.name,
        "descripcion" to descripcion,
        "icono" to tipo.icono,
        "jugadorId" to jugadorId,
        "timestamp" to ahora()
    )

    private fun goleadorRef(campeonatoId: String, jugadorId: String): DocumentReference? =
        if (campeonatoId.isBlank() || jugadorId.isBlank()) null
        else db.collection(Campos.COL_GOLEADORES).document("${campeonatoId}_$jugadorId")

    private fun posicionRef(campeonatoId: String, equipoId: String): DocumentReference =
        db.collection(Campos.COL_POSICIONES).document("${campeonatoId}_$equipoId")

    private fun avisar(mensaje: String) {
        _mensajes.trySend(mensaje)
    }

    private fun ahora(): Long = System.currentTimeMillis()

    private fun DocumentSnapshot.entero(campo: String): Int = (getLong(campo) ?: 0L).toInt()

    companion object {
        fun factoria(partidoId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { ArbitrajeViewModel(partidoId) }
        }
    }
}

// =============================================================================
// 3. MODELO DE ESTADO
// =============================================================================

internal object Campos {
    const val COL_PARTIDOS = "partidos_en_vivo"
    const val COL_JUGADORES = "jugadores_globales"
    const val COL_GOLEADORES = "goleadores"
    const val COL_POSICIONES = "tabla_posiciones"

    const val PUNTUACION_LOCAL = "puntuacionLocal"
    const val PUNTUACION_VISITANTE = "puntuacionVisitante"
    const val NOMBRE_LOCAL = "equipoLocalNombre"
    const val NOMBRE_VISITANTE = "equipoVisitanteNombre"
    const val EQUIPO_LOCAL_ID = "equipoLocalId"
    const val EQUIPO_VISITANTE_ID = "equipoVisitanteId"
    const val ESTADO = "estado"
    const val EVENTO_RECIENTE = "eventoReciente"
    const val DEPORTE_ID = "deporteId"
    const val CAMPEONATO_ID = "campeonatoId"
    const val LINK_TRANSMISION = "linkTransmision"

    const val AMARILLAS_LOCAL = "amarillasLocal"
    const val AMARILLAS_VISITANTE = "amarillasVisitante"
    const val ROJAS_LOCAL = "rojasLocal"
    const val ROJAS_VISITANTE = "rojasVisitante"
    const val CORNERS_LOCAL = "cornersLocal"
    const val CORNERS_VISITANTE = "cornersVisitante"

    const val TIMELINE = "lineaDeTimeline"
    const val MINUTO_ACTUAL = "minutoActual"
    const val TIEMPO_ANADIDO = "tiempoAnadido"
    const val CRONO_CORRIENDO = "estaCorriendoCronometro"

    const val CRONO_SEGUNDOS = "cronometroSegundosAcumulados"
    const val CRONO_INICIO = "cronometroInicio"

    const val RESULTADO_APLICADO = "resultadoAplicado"

    const val JUGADOR_SUSPENDIDO = "estaSuspendido"
    const val GOLES = "goles"
}

enum class EstadoPartido(val valorFirestore: String) {
    PROGRAMADO("Programado"),
    EN_CURSO("En Curso"),
    TERMINADO("Terminado");

    companion object {
        fun desde(valor: String?): EstadoPartido =
            values().firstOrNull { it.valorFirestore.equals(valor, ignoreCase = true) } ?: PROGRAMADO
    }
}

enum class TipoEvento(val icono: String) {
    INICIO("▶️"),
    GOL("⚽"),
    GOL_ANULADO("❌"),
    TARJETA_AMARILLA("🟨"),
    TARJETA_ROJA("🟥"),
    CORNER("🚩"),
    CAMBIO("🔄"),
    AVISO("📢"),
    FIN_PERIODO("⏸️"),
    FIN_PARTIDO("⏹️")
}

@Immutable
data class EventoPartido(
    val minuto: Int = 0,
    val tipo: String = "",
    val descripcion: String = "",
    val icono: String = "",
    val jugadorId: String = "",
    val timestamp: Long = 0L
) {
    companion object {
        private fun desdeMapa(mapa: Map<*, *>) = EventoPartido(
            minuto = (mapa["minuto"] as? Number)?.toInt() ?: 0,
            tipo = mapa["tipo"] as? String ?: "",
            descripcion = mapa["descripcion"] as? String ?: "",
            icono = mapa["icono"] as? String ?: "",
            jugadorId = mapa["jugadorId"] as? String ?: "",
            timestamp = (mapa["timestamp"] as? Number)?.toLong() ?: 0L
        )

        fun leerDe(snapshot: DocumentSnapshot): List<EventoPartido> =
            (snapshot.get(Campos.TIMELINE) as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.map(::desdeMapa)
                .orEmpty()
    }
}

@Immutable
data class Cronometro(
    val corriendo: Boolean = false,
    val segundosAcumulados: Long = 0L,
    val inicioEpochMillis: Long? = null,
    val tiempoAnadido: Int = 0
) {
    fun segundosEn(ahoraMillis: Long): Long =
        if (corriendo && inicioEpochMillis != null) {
            segundosAcumulados + ((ahoraMillis - inicioEpochMillis).coerceAtLeast(0L) / 1000L)
        } else {
            segundosAcumulados
        }
}

@Immutable
data class EstadisticasEquipo(
    val amarillas: Int = 0,
    val rojas: Int = 0,
    val corners: Int = 0
)

@Immutable
data class ArbitrajeUiState(
    val cargando: Boolean = true,
    val errorFatal: String? = null,
    val nombreLocal: String = "Local",
    val nombreVisitante: String = "Visitante",
    val equipoLocalId: String = "",
    val equipoVisitanteId: String = "",
    val deporteId: String = "futbol",
    val campeonatoId: String = "",
    val marcadorLocal: Int = 0,
    val marcadorVisitante: Int = 0,
    val estado: EstadoPartido = EstadoPartido.PROGRAMADO,
    val eventoReciente: String = "Esperando inicio...",
    val statsLocal: EstadisticasEquipo = EstadisticasEquipo(),
    val statsVisitante: EstadisticasEquipo = EstadisticasEquipo(),
    val cronometro: Cronometro = Cronometro(),
    val segundosTranscurridos: Long = 0L,
    val timeline: List<EventoPartido> = emptyList(),
    val jugadoresLocal: List<Jugador> = emptyList(),
    val jugadoresVisitante: List<Jugador> = emptyList(),
    val resultadoAplicado: Boolean = false
) {
    val puedeRegistrarEventos: Boolean get() = estado == EstadoPartido.EN_CURSO
    val partidoCerrado: Boolean get() = estado == EstadoPartido.TERMINADO
    val minutoActual: Int get() = (segundosTranscurridos / 60L).toInt()

    fun jugadoresDe(esLocal: Boolean): List<Jugador> =
        if (esLocal) jugadoresLocal else jugadoresVisitante

    fun esEquipoLocal(equipoId: String): Boolean =
        equipoLocalId.isNotEmpty() && equipoId == equipoLocalId
}