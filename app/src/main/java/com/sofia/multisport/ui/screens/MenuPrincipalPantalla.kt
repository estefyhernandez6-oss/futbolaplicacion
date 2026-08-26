package com.sofia.multisport.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuPrincipalPantalla(
    hayPartidoActivo: Boolean = true,
    onNavegarA: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Multisport ⚽",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Tarjeta dinámica de Árbitro en Vivo (Solo aparece si hay un partido activo)
            if (hayPartidoActivo) {
                item {
                    MenuCardEspecial(
                        titulo = "Árbitro en Vivo",
                        subtitulo = "¡Partido en curso! Toca para gestionar el encuentro",
                        icono = "⏱️",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { onNavegarA("arbitro_vivo") }
                    )
                }
            }

            // Opciones del Menú Principal con diseño flotante
            item {
                MenuCardOpcion(
                    titulo = "Ver App (Público)",
                    subtitulo = "Explora los marcadores y tablas en tiempo real",
                    icono = "🏟️",
                    onClick = { onNavegarA("app_publico") }
                )
            }
            item {
                MenuCardOpcion(
                    titulo = "Lanzar Contenido",
                    subtitulo = "Publica avisos, notas oficiales o anuncios",
                    icono = "📑",
                    onClick = { onNavegarA("lanzar_contenido") }
                )
            }
            item {
                MenuCardOpcion(
                    titulo = "Gestión Plantillas",
                    subtitulo = "Administra alineaciones y esquemas de equipos",
                    icono = "🔄",
                    onClick = { onNavegarA("gestion_plantillas") }
                )
            }
            item {
                MenuCardOpcion(
                    titulo = "Registro Equipos / Jugadores",
                    subtitulo = "Da de alta nuevos participantes y clubes",
                    icono = "⚽",
                    onClick = { onNavegarA("registro_equipos") }
                )
            }
            item {
                MenuCardOpcion(
                    titulo = "Jugadores y Equipos",
                    subtitulo = "Consulta la lista general registrada",
                    icono = "👥",
                    onClick = { onNavegarA("lista_equipos") }
                )
            }
        }
    }
}

// Tarjeta elegante y estándar para las opciones
@Composable
fun MenuCardOpcion(
    titulo: String,
    subtitulo: String,
    icono: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = icono,
                fontSize = 28.sp
            )
        }
    }
}

// Tarjeta especial y llamativa para cuando el arbitraje está activo
@Composable
fun MenuCardEspecial(
    titulo: String,
    subtitulo: String,
    icono: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "EN VIVO 🔴",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}