package com.sofia.multisport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.sofia.multisport.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerPantalla() {

    var pantallaActual by remember { mutableStateOf("home") }
    var sesionIniciada by remember { mutableStateOf(false) }

    // Partido que el arbitro esta dirigiendo. Antes se pasaba la cadena literal
    // "ID_DEL_PARTIDO" a ArbitrajePantalla: ese documento no existe en Firestore, asi
    // que cada transaccion del panel (gol, tarjeta, cronometro) fallaba y nada se
    // guardaba. Ahora el arbitro elige un partido real de la lista.
    var partidoEnArbitraje by remember { mutableStateOf<String?>(null) }

    // Pantallas que escriben en la base y por tanto exigen sesion iniciada. Las reglas
    // de Firestore lo exigen en el servidor; esto lo hace visible en la app en vez de
    // dejar que la escritura falle en silencio.
    val pantallasProtegidas = remember {
        setOf("arbitraje", "crear_contenido", "plantillas", "registro_general")
    }

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        sesionIniciada = currentUser != null
    }

    val titulos = mapOf(
        "home" to "🏟️ ARENA LIVE",
        "posiciones" to "📊 POSICIONES",
        "goleadores" to "⚽ GOLEADORES",
        "arbitraje" to "⏱️ ARBITRAJE EN VIVO",
        "crear_contenido" to "📰 NUEVA PUBLICACIÓN",
        "plantillas" to "🔄 GESTIÓN DE PLANTILLAS",
        "login" to "🔐 ACCESO PANEL",
        "registro_general" to "⚙️ REGISTRO GLOBAL",
        "jugadores_equipos" to "👥 JUGADORES Y EQUIPOS"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (sesionIniciada) "⚙️ PANEL MULTISPORT" else "🔐 MENÚ GENERAL",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                Divider(
                    color = Color.LightGray,
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Ver App (Público) 🏟️", color = Color.Black) },
                    selected = pantallaActual == "home",
                    onClick = {
                        pantallaActual = "home"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Árbitro en Vivo ⏱️", color = Color.Black) },
                    selected = pantallaActual == "arbitraje",
                    onClick = {
                        pantallaActual = "arbitraje"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Lanzar Contenido 📰", color = Color.Black) },
                    selected = pantallaActual == "crear_contenido",
                    onClick = {
                        pantallaActual = "crear_contenido"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Gestión Plantillas 🔄", color = Color.Black) },
                    selected = pantallaActual == "plantillas",
                    onClick = {
                        pantallaActual = "plantillas"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Registro Equipos/Jug ⚽", color = Color.Black) },
                    selected = pantallaActual == "registro_general",
                    onClick = {
                        pantallaActual = "registro_general"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Jugadores y Equipos 👥", color = Color.Black) },
                    selected = pantallaActual == "jugadores_equipos",
                    onClick = {
                        pantallaActual = "jugadores_equipos"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (sesionIniciada) {
                    NavigationDrawerItem(
                        label = { Text("Cerrar Sesión 🚪", color = Color(0xFFD32F2F)) },
                        selected = false,
                        onClick = {
                            auth.signOut()
                            sesionIniciada = false
                            partidoEnArbitraje = null
                            pantallaActual = "home"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else {
                    NavigationDrawerItem(
                        label = { Text("Iniciar Sesión 🔐", color = Color(0xFF1976D2)) },
                        selected = pantallaActual == "login",
                        onClick = {
                            pantallaActual = "login"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = titulos[pantallaActual] ?: "🏆 MULTISPORT MANAGER",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF1A1A2E)
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("🛠️", fontSize = 20.sp)
                        }
                    },
                    actions = {
                        if (pantallaActual !in listOf("home", "posiciones", "goleadores")) {
                            IconButton(onClick = { pantallaActual = "home" }) {
                                Text("🏟️", fontSize = 20.sp)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (pantallaActual in listOf("home", "posiciones", "goleadores")) {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            icon = { Text("🏟️", fontSize = 20.sp) },
                            label = { Text("Inicio", color = Color.Black) },
                            selected = pantallaActual == "home",
                            onClick = { pantallaActual = "home" }
                        )
                        NavigationBarItem(
                            icon = { Text("📊", fontSize = 20.sp) },
                            label = { Text("Posiciones", color = Color.Black) },
                            selected = pantallaActual == "posiciones",
                            onClick = { pantallaActual = "posiciones" }
                        )
                        NavigationBarItem(
                            icon = { Text("⚽", fontSize = 20.sp) },
                            label = { Text("Goleadores", color = Color.Black) },
                            selected = pantallaActual == "goleadores",
                            onClick = { pantallaActual = "goleadores" }
                        )
                    }
                }
            }
        ) { paddingInterno ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingInterno)
            ) {
                if (pantallaActual in pantallasProtegidas && !sesionIniciada) {
                    AccesoRequerido(onIrALogin = { pantallaActual = "login" })
                } else when (pantallaActual) {
                    "home" -> HomeUsuarioPantalla(onNavegarARegistro = { pantallaActual = "registro" })
                    "registro" -> RegistroUsuarioPantalla(onRegistroExitoso = { pantallaActual = "home" })
                    "crear_contenido" -> CrearContenidoPantalla()
                    "posiciones" -> TablaPosicionesPantalla()
                    "goleadores" -> TablaGoleadoresPantalla(deporteId = "futbol", listaGoleadores = emptyList())
                    "login" -> LoginPantalla(onLoginSuccess = { _ -> sesionIniciada = true; pantallaActual = "home" })
                    "plantillas" -> GestionPlantillaPantalla()
                    "registro_general" -> RegistroAdminPantalla()
                    "jugadores_equipos" -> ListaEquiposPantalla()

                    "arbitraje" -> {
                        val idPartido = partidoEnArbitraje
                        if (idPartido == null) {
                            ListaPartidosArbitroPantalla(
                                onPartidoSeleccionado = { partidoEnArbitraje = it }
                            )
                        } else {
                            ArbitrajePantalla(
                                partidoId = idPartido,
                                onBack = { partidoEnArbitraje = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Muro para las pantallas que escriben en Firestore.
 *
 * El menu lateral daba acceso al panel de administracion, al de arbitraje y al gestor
 * de contenido sin haber iniciado sesion. Las reglas de Firestore rechazan esas
 * escrituras (`request.auth != null`), asi que el formulario se llenaba y el dato se
 * perdia sin explicacion. Este aviso lo dice antes de que ocurra.
 */
@Composable
private fun AccesoRequerido(onIrALogin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14081E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔐", fontSize = 44.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Necesitas iniciar sesión",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta sección escribe en la base de datos y solo funciona con una cuenta de árbitro o administrador.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onIrALogin,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("IR AL LOGIN", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
