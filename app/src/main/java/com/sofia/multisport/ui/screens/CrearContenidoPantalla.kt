package com.sofia.multisport.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.sofia.multisport.data.models.Noticia
import com.sofia.multisport.data.models.Publicidad
import java.util.UUID

/**
 * Gestor de contenido: noticias y banners de publicidad.
 *
 * Los dos botones de guardado estaban vacios: el formulario se llenaba, el boton
 * respondia al toque y no se escribia nada en Firestore. Aqui quedan implementados
 * contra las colecciones que ya lee [HomeUsuarioPantalla]: `noticias` y `publicidades`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearContenidoPantalla() {
    val contexto = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    var pestanaSeleccionada by remember { mutableStateOf(0) }
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var enlaceDestino by remember { mutableStateOf("") }
    var nombreNegocio by remember { mutableStateOf("") }

    // `deporteId` decide en que pestana del home aparece la noticia. HomeUsuarioPantalla
    // consulta whereIn("deporteId", listOf(deporteActual, "global")), asi que "global"
    // la muestra en todas.
    val deportes = remember {
        listOf(
            "global" to "Todos",
            "futbol" to "Fútbol",
            "basquet" to "Básquet",
            "ecuavoley" to "Ecuavóley"
        )
    }
    var deporteSeleccionado by remember { mutableStateOf("global") }

    var guardandoNoticia by remember { mutableStateOf(false) }
    var subiendoBanner by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imagenUri = uri
    }

    // Paleta de colores Premium
    val colorFondo = Color(0xFF0F172A)
    val colorTarjeta = Color(0xFF1E293B)
    val colorAcento = Color(0xFF00F5D4)
    val colorRojo = Color(0xFFE94560)
    val colorTextoSecundario = Color(0xFF94A3B8)

    // Estilo para los campos de texto
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.LightGray,
        focusedContainerColor = colorFondo,
        unfocusedContainerColor = colorFondo,
        focusedBorderColor = colorAcento,
        unfocusedBorderColor = Color.DarkGray,
        focusedLabelColor = colorAcento,
        unfocusedLabelColor = Color.Gray,
        cursorColor = colorAcento
    )

    /** Publica la noticia en `noticias/{id}`. */
    fun publicarNoticia() {
        if (titulo.isBlank() || contenido.isBlank()) {
            Toast.makeText(contexto, "El titular y el cuerpo son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        val id = UUID.randomUUID().toString()
        val noticia = Noticia(
            id = id,
            titulo = titulo.trim(),
            contenido = contenido.trim(),
            deporteId = deporteSeleccionado,
            fecha = System.currentTimeMillis()
        )
        guardandoNoticia = true
        db.collection("noticias").document(id).set(noticia)
            .addOnSuccessListener {
                guardandoNoticia = false
                titulo = ""
                contenido = ""
                Toast.makeText(contexto, "Noticia publicada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                guardandoNoticia = false
                Log.e("GUARDADO", "noticias/$id no se guardo", e)
                Toast.makeText(
                    contexto,
                    "No se publicó la noticia: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Sube la imagen a Storage y, solo cuando tiene la URL de descarga, escribe el
     * documento en `publicidades/{id}`. El orden importa: si se guardara primero el
     * documento, la app mostraria un banner con `imagenUrl` vacio.
     */
    fun subirBanner() {
        val uri = imagenUri
        if (uri == null) {
            Toast.makeText(contexto, "Elige primero una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        if (nombreNegocio.isBlank()) {
            Toast.makeText(contexto, "Escribe el nombre del negocio", Toast.LENGTH_SHORT).show()
            return
        }

        val id = UUID.randomUUID().toString()
        val referencia = storage.reference.child("publicidades/$id.jpg")
        subiendoBanner = true

        referencia.putFile(uri)
            .continueWithTask { tarea ->
                // Propaga el fallo de la subida en vez de pedir la URL de un archivo inexistente.
                if (!tarea.isSuccessful) throw tarea.exception ?: IllegalStateException("Fallo la subida")
                referencia.downloadUrl
            }
            .addOnSuccessListener { url ->
                val publicidad = Publicidad(
                    id = id,
                    titulo = nombreNegocio.trim(),
                    imagenUrl = url.toString(),
                    descripcion = "",
                    linkEnlace = enlaceDestino.trim()
                )
                db.collection("publicidades").document(id).set(publicidad)
                    .addOnSuccessListener {
                        subiendoBanner = false
                        nombreNegocio = ""
                        enlaceDestino = ""
                        imagenUri = null
                        Toast.makeText(contexto, "Banner activado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        subiendoBanner = false
                        Log.e("GUARDADO", "publicidades/$id no se guardo", e)
                        Toast.makeText(
                            contexto,
                            "La imagen subió pero no se guardó el banner: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                subiendoBanner = false
                Log.e("GUARDADO", "storage publicidades/$id.jpg fallo", e)
                Toast.makeText(
                    contexto,
                    "No se subió la imagen: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
    ) {
        // Cabecera Elegante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorTarjeta)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GESTOR DE CONTENIDO",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        // TABS ESTILIZADOS
        TabRow(
            selectedTabIndex = pestanaSeleccionada,
            containerColor = colorTarjeta,
            contentColor = colorAcento,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[pestanaSeleccionada])
                        .height(3.dp)
                        .background(colorAcento, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
            },
            divider = { Divider(color = colorFondo) }
        ) {
            Tab(
                selected = pestanaSeleccionada == 0,
                onClick = { pestanaSeleccionada = 0 },
                text = { Text("NOTICIAS 📰", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (pestanaSeleccionada == 0) colorAcento else colorTextoSecundario) }
            )
            Tab(
                selected = pestanaSeleccionada == 1,
                onClick = { pestanaSeleccionada = 1 },
                text = { Text("ANUNCIOS 🖼️", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (pestanaSeleccionada == 1) colorAcento else colorTextoSecundario) }
            )
        }

        // CONTENEDOR PRINCIPAL ANIMADO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .animateContentSize(), // Animación fluida al cambiar de pestaña
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (pestanaSeleccionada == 0) {
                // PANEL DE NOTICIAS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = colorAcento, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Redactar Comunicado", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("Titular de la Noticia") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = contenido,
                            onValueChange = { contenido = it },
                            label = { Text("Cuerpo del mensaje...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            maxLines = 8
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("¿En qué deporte se publica?", color = colorTextoSecundario, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            deportes.forEach { (idDeporte, etiqueta) ->
                                FilterChip(
                                    selected = deporteSeleccionado == idDeporte,
                                    onClick = { deporteSeleccionado = idDeporte },
                                    label = { Text(etiqueta, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colorAcento,
                                        selectedLabelColor = Color.Black,
                                        labelColor = colorTextoSecundario
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { publicarNoticia() },
                            enabled = !guardandoNoticia,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorAcento),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (guardandoNoticia) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PUBLICAR NOTICIA", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // PANEL DE ANUNCIOS (BANNERS)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = colorRojo, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Subir Pauta Publicitaria", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = nombreNegocio,
                            onValueChange = { nombreNegocio = it },
                            label = { Text("Nombre del negocio") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = enlaceDestino,
                            onValueChange = { enlaceDestino = it },
                            label = { Text("Enlace al hacer clic (URL)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // ZONA DE CARGA DE IMAGEN ESTILIZADA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorFondo)
                                .border(BorderStroke(2.dp, if (imagenUri != null) colorAcento else Color.DarkGray), RoundedCornerShape(12.dp))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val uriActual = imagenUri
                            if (uriActual != null) {
                                // Vista previa real de lo que se va a subir.
                                AsyncImage(
                                    model = uriActual,
                                    contentDescription = "Vista previa del banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorTarjeta,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = colorTextoSecundario, modifier = Modifier.padding(14.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Toca para explorar galería", color = colorTextoSecundario, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { subirBanner() },
                            enabled = !subiendoBanner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorRojo),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (subiendoBanner) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SUBIR Y ACTIVAR BANNER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
