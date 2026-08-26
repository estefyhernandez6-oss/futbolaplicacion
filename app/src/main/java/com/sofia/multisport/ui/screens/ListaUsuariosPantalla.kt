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
import com.sofia.multisport.ViewModel.ListaUsuariosViewModel
import com.sofia.multisport.data.models.UsuarioApp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaUsuariosPantalla(
    viewModel: ListaUsuariosViewModel = remember { ListaUsuariosViewModel() }
) {
    val usuarios = viewModel.usuariosList
    val cargando = viewModel.cargando

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usuarios Registrados") }
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
            } else if (usuarios.isEmpty()) {
                Text(
                    text = "No hay usuarios registrados todavía.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usuarios) { usuario ->
                        UsuarioItemCard(usuario = usuario)
                    }
                }
            }
        }
    }
}

@Composable
fun UsuarioItemCard(usuario: UsuarioApp) {
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
                text = usuario.nombre.ifEmpty { "Sin nombre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Correo: ${usuario.correo.ifEmpty { "No especificado" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Equipo favorito: ${usuario.equipoFavoritoNombre.ifEmpty { "Ninguno" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (usuario.esRepresentanteEquipo) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⭐ Representante de Equipo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}