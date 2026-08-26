package com.sofia.multisport.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sofia.multisport.data.models.UsuarioApp
import java.util.UUID

@Composable
fun RegistroUsuarioPantalla(
    onRegistroExitoso: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = FirebaseFirestore.getInstance()
    val contexto = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var equipoSeleccionado by remember { mutableStateOf("") }
    var quiereRegistrarEquipo by remember { mutableStateOf(false) }
    var nombreEquipoNuevo by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14081E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¡REGISTRO DE USUARIO! 🏆",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = equipoSeleccionado,
            onValueChange = { equipoSeleccionado = it },
            label = { Text("¿A qué equipo apoyas?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = quiereRegistrarEquipo,
                onCheckedChange = { quiereRegistrarEquipo = it }
            )
            Text(
                text = "Quiero registrar un nuevo equipo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (quiereRegistrarEquipo) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nombreEquipoNuevo,
                onValueChange = { nombreEquipoNuevo = it },
                label = { Text("Nombre del Nuevo Equipo a Inscribir") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF311547), unfocusedContainerColor = Color(0xFF311547), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (correo.isBlank() || contrasena.isBlank() || nombre.isBlank()) {
                    Toast.makeText(contexto, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                cargando = true
                auth.createUserWithEmailAndPassword(correo.trim(), contrasena.trim())
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: UUID.randomUUID().toString()
                        val nuevoUsuario = UsuarioApp(
                            id = uid,
                            nombre = nombre,
                            correo = correo.trim(),
                            equipoFavoritoNombre = equipoSeleccionado,
                            esRepresentanteEquipo = quiereRegistrarEquipo
                        )

                        // Guardar perfil en usuarios/{uid} con .set() (SIN contraseña)
                        db.collection("usuarios").document(uid).set(nuevoUsuario)
                            .addOnSuccessListener {
                                if (quiereRegistrarEquipo && nombreEquipoNuevo.isNotBlank()) {
                                    val equipoId = UUID.randomUUID().toString()
                                    val equipoData = mapOf(
                                        "id" to equipoId,
                                        "nombre" to nombreEquipoNuevo,
                                        "representante" to nombre,
                                        "estado" to "Pendiente de Aprobación"
                                    )
                                    db.collection("equipos_globales").document(equipoId).set(equipoData)
                                        .addOnFailureListener { e ->
                                            // El perfil se guardo pero el equipo no: hay que
                                            // decirlo, antes se perdia en silencio.
                                            Log.e("GUARDADO", "equipos_globales/$equipoId no se guardo", e)
                                            Toast.makeText(
                                                contexto,
                                                "Tu cuenta se creó, pero el equipo no se inscribió: ${e.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }

                                cargando = false
                                Toast.makeText(contexto, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                                onRegistroExitoso()
                            }
                            .addOnFailureListener {
                                cargando = false
                                Toast.makeText(contexto, "Error al guardar perfil", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        cargando = false
                        Toast.makeText(contexto, "Error Auth: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (cargando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "REGISTRARSE Y CONTINUAR", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}