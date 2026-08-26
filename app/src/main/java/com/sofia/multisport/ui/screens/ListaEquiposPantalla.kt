package com.sofia.multisport.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sofia.multisport.ViewModel.ListaEquiposViewModel
import com.sofia.multisport.data.models.Equipo


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEquiposPantalla(
    viewModel: ListaEquiposViewModel = remember { ListaEquiposViewModel() }
) {
    val equipos = viewModel.equiposList
    val cargando = viewModel.cargando

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipos Registrados") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (equipos.isEmpty()) {
                Text(
                    text = "No hay equipos registrados todavía.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(equipos) { equipo: Equipo ->
                        EquipoItemCard(equipo = equipo)
                    }
                }
            }
        }
    }
}

@Composable
fun EquipoItemCard(equipo: Equipo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = equipo.nombre.ifEmpty { "Sin nombre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Categoría: ${equipo.categoriaId}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Representante: ${equipo.representante.ifEmpty { "No especificado" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Estado: ${equipo.estado.ifEmpty { "Pendiente" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}