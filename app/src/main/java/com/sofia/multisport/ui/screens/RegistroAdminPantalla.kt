package com.sofia.multisport.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroAdminPantalla(
    modifier: Modifier = Modifier
) {
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    val titulosTabs = remember { listOf("Equipos", "Jugadores", "Campeonatos", "Partidos", "Usuarios") }

    val colorBgApp = Color(0xFF14081E)
    val colorTopBar = Color(0xFF26004C)
    val colorCardBg = Color(0xFF311547)
    val colorAccentYellow = Color(0xFFFFC107)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorBgApp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorTopBar)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PANEL DE CONTROL GENERAL",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        ScrollableTabRow(
            selectedTabIndex = tabSeleccionada,
            containerColor = colorCardBg,
            contentColor = colorAccentYellow,
            edgePadding = 16.dp,
            divider = { Divider(color = Color(0xFF452B5B)) }
        ) {
            titulosTabs.forEachIndexed { indice, titulo ->
                Tab(
                    selected = tabSeleccionada == indice,
                    onClick = { tabSeleccionada = indice },
                    text = {
                        Text(
                            titulo,
                            fontWeight = if(tabSeleccionada == indice) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if(tabSeleccionada == indice) Color.White else Color(0xFFA191B0)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (tabSeleccionada) {
                0 -> FormularioEquipo()
                1 -> FormularioJugador()
                2 -> GestionCampeonatosTab()
                3 -> FormularioPartido()
                4 -> FormularioGestionUsuarios()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionCampeonatosTab() {
    val db = FirebaseFirestore.getInstance()
    var listaCampeonatos by remember { mutableStateOf(emptyList<Campeonato>()) }
    var listaEquipos by remember { mutableStateOf(emptyList<Equipo>()) }
    var campExpandido by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("campeonatos").addSnapshotListener { snapshot, error ->
            // Antes el error se descartaba: si las reglas denegaban la lectura o faltaba
            // un indice, la lista salia vacia sin explicacion.
            if (error != null) {
                Log.e("LECTURA", "campeonatos: ${error.message}", error)
                return@addSnapshotListener
            }
            listaCampeonatos = snapshot?.toObjects(Campeonato::class.java) ?: emptyList()
        }
        db.collection("equipos_globales").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("LECTURA", "equipos_globales: ${error.message}", error)
                return@addSnapshotListener
            }
            listaEquipos = snapshot?.toObjects(Equipo::class.java) ?: emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Gestión de Torneos y Participantes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            FormularioCampeonatoInner()
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF452B5B))
        }

        items(listaCampeonatos, key = { it.id }) { camp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable { campExpandido = if (campExpandido == camp.id) null else camp.id },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF311547)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(camp.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Text(
                                "Categoría: ${camp.categoriaId.uppercase()} | Año: ${camp.anio}",
                                fontSize = 12.sp,
                                color = Color(0xFFA191B0)
                            )
                        }
                        Icon(
                            imageVector = if (campExpandido == camp.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Desplegar",
                            tint = Color.White
                        )
                    }

                    if (campExpandido == camp.id) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Equipos Inscritos (Categoría ${camp.categoriaId.uppercase()}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFFFC107)
                        )
                        val equiposDeCategoria = listaEquipos.filter { it.categoriaId == camp.categoriaId }

                        if (equiposDeCategoria.isEmpty()) {
                            Text(
                                "No hay equipos registrados en esta categoría.",
                                fontSize = 12.sp,
                                color = Color(0xFFE53935),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            equiposDeCategoria.forEach { eq ->
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Text(eq.nombre, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioCampeonatoInner() {
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("2026") }
    var deporteId by remember { mutableStateOf("futbol") }
    var categoriaSeleccionada by remember { mutableStateOf("sub_12") }
    val categorias = remember { listOf("sub_8", "sub_10", "sub_12", "sub_14", "sub_16", "libre") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = deporteId == "futbol",
                onClick = { deporteId = "futbol" },
                label = { Text("Fútbol") }
            )
            FilterChip(
                selected = deporteId == "basquet",
                onClick = { deporteId = "basquet" },
                label = { Text("Básquet") }
            )
        }

        Text("Categoría:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categorias) { cat ->
                FilterChip(
                    selected = categoriaSeleccionada == cat,
                    onClick = { categoriaSeleccionada = cat },
                    label = { Text(cat.uppercase()) }
                )
            }
        }

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del Campeonato") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF14081E), unfocusedContainerColor = Color(0xFF14081E), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        OutlinedTextField(
            value = anio,
            onValueChange = { anio = it },
            label = { Text("Año") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF14081E), unfocusedContainerColor = Color(0xFF14081E), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Button(
            onClick = {
                if (nombre.isNotBlank()) {
                    val camp = Campeonato(
                        id = UUID.randomUUID().toString(),
                        nombre = nombre,
                        anio = anio.toIntOrNull() ?: 2026,
                        categoriaId = categoriaSeleccionada
                    )
                    db.collection("campeonatos").document(camp.id).set(camp)
                        .addOnSuccessListener {
                            Toast.makeText(contexto, "Campeonato creado con éxito", Toast.LENGTH_SHORT).show()
                            nombre = ""
                        }
                        .addOnFailureListener { e ->
                            // Sin este listener el fallo era invisible: el boton parecia no
                            // hacer nada y el dato nunca llegaba a Firestore.
                            Log.e("GUARDADO", "campeonatos/${camp.id} no se guardo", e)
                            Toast.makeText(
                                contexto,
                                "No se guardó el campeonato: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(contexto, "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("CREAR CAMPEONATO", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioEquipo() {
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current
    var nombreEquipo by remember { mutableStateOf("") }
    val coloresPredefinidos = remember {
        listOf(
            "Rojo" to "#E94560",
            "Azul" to "#0F3460",
            "Amarillo" to "#FFD200",
            "Verde" to "#4CD137",
            "Blanco" to "#FFFFFF",
            "Negro" to "#1E1E24"
        )
    }
    var colorSeleccionadoHex by remember { mutableStateOf("#E94560") }
    var categoriaSeleccionada by remember { mutableStateOf("sub_12") }
    val categorias = remember { listOf("sub_8", "sub_10", "sub_12", "sub_14", "sub_16", "libre") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Registro de Nuevo Club", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Text("Categoría del Equipo:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA191B0))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categorias) { cat ->
                FilterChip(
                    selected = categoriaSeleccionada == cat,
                    onClick = { categoriaSeleccionada = cat },
                    label = { Text(cat.uppercase()) }
                )
            }
        }

        OutlinedTextField(
            value = nombreEquipo,
            onValueChange = { nombreEquipo = it },
            label = { Text("Nombre del Equipo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Text("Color principal:", fontSize = 13.sp, color = Color(0xFFA191B0))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            coloresPredefinidos.forEach { (_, hex) ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(hex.toColorInt()), CircleShape)
                        .border(
                            width = if (colorSeleccionadoHex == hex) 3.dp else 1.dp,
                            color = if (colorSeleccionadoHex == hex) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { colorSeleccionadoHex = hex }
                )
            }
        }

        Button(
            onClick = {
                if (nombreEquipo.isNotBlank()) {
                    val nuevoEquipo = Equipo(
                        id = UUID.randomUUID().toString(),
                        nombre = nombreEquipo,
                        colorPrincipal = colorSeleccionadoHex,
                        categoriaId = categoriaSeleccionada,
                        estado = "Aprobado"
                    )
                    db.collection("equipos_globales").document(nuevoEquipo.id).set(nuevoEquipo)
                        .addOnSuccessListener {
                            Toast.makeText(contexto, "¡Equipo guardado con éxito!", Toast.LENGTH_SHORT).show()
                            nombreEquipo = ""
                        }
                        .addOnFailureListener { e ->
                            Log.e("GUARDADO", "equipos_globales/${nuevoEquipo.id} no se guardo", e)
                            Toast.makeText(
                                contexto,
                                "No se guardó el equipo: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(contexto, "Completa el nombre del equipo", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("GUARDAR EQUIPO", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun FormularioJugador() {
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current
    var nombreJugador by remember { mutableStateOf("") }
    var cedulaJugador by remember { mutableStateOf("") }
    var edadJugador by remember { mutableStateOf("") }
    var dorsalJugador by remember { mutableStateOf("") }
    var listaEquipos by remember { mutableStateOf(emptyList<Equipo>()) }
    var menuExpandido by remember { mutableStateOf(false) }
    var equipoSeleccionado by remember { mutableStateOf<Equipo?>(null) }

    LaunchedEffect(Unit) {
        db.collection("equipos_globales").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) listaEquipos = snapshot.toObjects(Equipo::class.java)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Registro de Deportista", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        OutlinedTextField(
            value = nombreJugador,
            onValueChange = { nombreJugador = it },
            label = { Text("Nombre Completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        OutlinedTextField(
            value = cedulaJugador,
            onValueChange = { cedulaJugador = it },
            label = { Text("Nº Cédula") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = edadJugador,
                onValueChange = { edadJugador = it },
                label = { Text("Edad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = dorsalJugador,
                onValueChange = { dorsalJugador = it },
                label = { Text("Dorsal (Nº)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { menuExpandido = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(text = equipoSeleccionado?.nombre ?: "Seleccionar Equipo ➔", color = Color.White)
            }
            DropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false },
                modifier = Modifier.background(Color(0xFF311547))
            ) {
                listaEquipos.forEach { equipo ->
                    DropdownMenuItem(
                        text = { Text(equipo.nombre, color = Color.White) },
                        onClick = {
                            equipoSeleccionado = equipo
                            menuExpandido = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                if (nombreJugador.isNotBlank() && equipoSeleccionado != null) {
                    val nuevoJugador = Jugador(
                        id = UUID.randomUUID().toString(),
                        nombre = nombreJugador,
                        cedula = cedulaJugador,
                        edad = edadJugador.toIntOrNull() ?: 0,
                        dorsal = dorsalJugador.toIntOrNull() ?: 0,
                        equipoId = equipoSeleccionado!!.id
                    )
                    db.collection("jugadores_globales").document(nuevoJugador.id).set(nuevoJugador)
                        .addOnSuccessListener {
                            Toast.makeText(contexto, "¡Jugador registrado correctamente!", Toast.LENGTH_SHORT).show()
                            nombreJugador = ""
                            cedulaJugador = ""
                            edadJugador = ""
                            dorsalJugador = ""
                            equipoSeleccionado = null
                        }
                        .addOnFailureListener { e ->
                            Log.e("GUARDADO", "jugadores_globales/${nuevoJugador.id} no se guardo", e)
                            Toast.makeText(
                                contexto,
                                "No se registró el jugador: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(contexto, "Faltan campos obligatorios o equipo", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("REGISTRAR JUGADOR", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPartido() {
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current
    var listaEquipos by remember { mutableStateOf(emptyList<Equipo>()) }
    var listaCampeonatos by remember { mutableStateOf(emptyList<Campeonato>()) }

    var localSeleccionado by remember { mutableStateOf<Equipo?>(null) }
    var visitanteSeleccionado by remember { mutableStateOf<Equipo?>(null) }
    var campeonatoSeleccionado by remember { mutableStateOf<Campeonato?>(null) }

    var cancha by remember { mutableStateOf("") }
    var fechaPartido by remember { mutableStateOf("") }
    var horaPartido by remember { mutableStateOf("") }
    var linkTransmision by remember { mutableStateOf("") }
    var deporteId by remember { mutableStateOf("futbol") }

    var expLocal by remember { mutableStateOf(false) }
    var expVisitante by remember { mutableStateOf(false) }
    var expCamp by remember { mutableStateOf(false) }

    LaunchedEffect(campeonatoSeleccionado) {
        db.collection("equipos_globales").get().addOnSuccessListener { snapshot ->
            val todos = snapshot.toObjects(Equipo::class.java)
            listaEquipos = if (campeonatoSeleccionado != null) {
                todos.filter { it.categoriaId == campeonatoSeleccionado!!.categoriaId }
            } else {
                todos
            }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("campeonatos").get().addOnSuccessListener { snapshot ->
            listaCampeonatos = snapshot.toObjects(Campeonato::class.java)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Programar Encuentro", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = deporteId == "futbol",
                onClick = { deporteId = "futbol" },
                label = { Text("Fútbol") }
            )
            FilterChip(
                selected = deporteId == "basquet",
                onClick = { deporteId = "basquet" },
                label = { Text("Básquet") }
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expCamp = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text(campeonatoSeleccionado?.nombre ?: "🏆 Seleccionar Campeonato")
            }
            DropdownMenu(expanded = expCamp, onDismissRequest = { expCamp = false }, modifier = Modifier.background(Color(0xFF311547))) {
                listaCampeonatos.forEach { camp ->
                    DropdownMenuItem(
                        text = { Text(camp.nombre, color = Color.White) },
                        onClick = {
                            campeonatoSeleccionado = camp
                            expCamp = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = cancha,
            onValueChange = { cancha = it },
            label = { Text("Nombre de la Cancha") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fechaPartido,
                onValueChange = { fechaPartido = it },
                label = { Text("Fecha (dd/mm)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = horaPartido,
                onValueChange = { horaPartido = it },
                label = { Text("Hora (hh:mm)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { expLocal = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Text(localSeleccionado?.nombre ?: "Local", maxLines = 1)
                }
                DropdownMenu(expanded = expLocal, onDismissRequest = { expLocal = false }, modifier = Modifier.background(Color(0xFF311547))) {
                    listaEquipos.forEach { eq ->
                        DropdownMenuItem(
                            text = { Text(eq.nombre, color = Color.White) },
                            onClick = {
                                localSeleccionado = eq
                                expLocal = false
                            }
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { expVisitante = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Text(visitanteSeleccionado?.nombre ?: "Visitante", maxLines = 1)
                }
                DropdownMenu(expanded = expVisitante, onDismissRequest = { expVisitante = false }, modifier = Modifier.background(Color(0xFF311547))) {
                    listaEquipos.forEach { eq ->
                        DropdownMenuItem(
                            text = { Text(eq.nombre, color = Color.White) },
                            onClick = {
                                visitanteSeleccionado = eq
                                expVisitante = false
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                if (localSeleccionado != null && visitanteSeleccionado != null && campeonatoSeleccionado != null) {
                    val partidoId = UUID.randomUUID().toString()
                    val nuevoPartido = Partido(
                        id = partidoId,
                        deporteId = deporteId,
                        campeonatoId = campeonatoSeleccionado!!.id,
                        equipoLocalId = localSeleccionado!!.id,
                        equipoLocalNombre = localSeleccionado!!.nombre,
                        equipoVisitanteId = visitanteSeleccionado!!.id,
                        equipoVisitanteNombre = visitanteSeleccionado!!.nombre,
                        cancha = cancha,
                        fechaString = fechaPartido,
                        horaString = horaPartido,
                        linkTransmision = linkTransmision,
                        estado = "Programado",
                        eventoReciente = "Próximo: $fechaPartido a las $horaPartido"
                    )
                    db.collection("partidos_en_vivo").document(partidoId).set(nuevoPartido)
                        .addOnSuccessListener {
                            Toast.makeText(contexto, "¡Partido publicado con éxito!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("GUARDADO", "partidos_en_vivo/$partidoId no se guardo", e)
                            Toast.makeText(
                                contexto,
                                "No se publicó el partido: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(contexto, "Faltan equipos o campeonato seleccionado", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("GUARDAR Y PUBLICAR", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioGestionUsuarios() {
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf("arbitro") }
    var listaUsuarios by remember { mutableStateOf(emptyList<Map<String, String>>()) }

    LaunchedEffect(Unit) {
        db.collection("usuarios").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            listaUsuarios = snapshot?.documents?.map {
                mapOf(
                    "id" to it.id,
                    "nombre" to (it.getString("nombre") ?: "Sin nombre"),
                    "rol" to (it.getString("rol") ?: "arbitro"),
                    "email" to (it.getString("email") ?: "")
                )
            } ?: emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Crear Nuevo Acceso", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo del Usuario") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña Provisional") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = rolSeleccionado == "arbitro",
                    onClick = { rolSeleccionado = "arbitro" },
                    label = { Text("Árbitro") }
                )
                FilterChip(
                    selected = rolSeleccionado == "admin",
                    onClick = { rolSeleccionado = "admin" },
                    label = { Text("Administrador") }
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (email.isNotBlank() && nombre.isNotBlank()) {
                        val data = mapOf(
                            "nombre" to nombre,
                            "email" to email.trim(),
                            "rol" to rolSeleccionado
                        )
                        db.collection("usuarios").add(data)
                            .addOnSuccessListener {
                                Toast.makeText(contexto, "Perfil de $nombre creado con éxito.", Toast.LENGTH_LONG).show()
                                nombre = ""
                                email = ""
                                password = ""
                            }
                            .addOnFailureListener { e ->
                                Log.e("GUARDADO", "usuarios no se guardo", e)
                                Toast.makeText(
                                    contexto,
                                    "No se creó el perfil: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } else {
                        Toast.makeText(contexto, "Nombre y Email son obligatorios", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("REGISTRAR PERFIL", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF452B5B))
            Text("Personal con Acceso", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
        }

        items(listaUsuarios) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF311547))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user["nombre"] ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text(user["email"] ?: "", fontSize = 11.sp, color = Color(0xFFA191B0))
                    }
                }
            }
        }
    }
}