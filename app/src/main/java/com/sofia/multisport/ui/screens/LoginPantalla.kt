package com.sofia.multisport.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginPantalla(
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var email by remember {
        mutableStateOf("")
    }

    var contrasena by remember {
        mutableStateOf("")
    }

    var mostrarContrasena by remember {
        mutableStateOf(false)
    }

    var cargando by remember {
        mutableStateOf(false)
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    val contexto = LocalContext.current

    val realizarLogin = {

        val correoLimpio = email.trim()
        val contrasenaLimpia = contrasena.trim()

        if (
            correoLimpio.isNotBlank() &&
            contrasenaLimpia.isNotBlank()
        ) {

            cargando = true

            auth.signInWithEmailAndPassword(
                correoLimpio,
                contrasenaLimpia
            )

                .addOnSuccessListener { result ->

                    val uid = result.user?.uid

                    if (uid != null) {

                        db.collection("usuarios")
                            .document(uid)
                            .get()

                            .addOnSuccessListener { doc ->

                                cargando = false

                                if (doc.exists()) {

                                    val rol =
                                        doc.getString("rol")
                                            ?: "arbitro"

                                    onLoginSuccess(rol)

                                } else {

                                    Toast.makeText(
                                        contexto,
                                        "El usuario no tiene un perfil registrado en Firestore",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            .addOnFailureListener { e ->

                                cargando = false

                                Toast.makeText(
                                    contexto,
                                    "Error Firestore: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                    } else {

                        cargando = false
                    }
                }

                .addOnFailureListener { e ->

                    cargando = false

                    Toast.makeText(
                        contexto,
                        "Error Auth: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } else {

            Toast.makeText(
                contexto,
                "Complete todos los campos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "ACCESO RESTRINGIDO",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Ingrese con su correo institucional",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        OutlinedTextField(
            value = email,

            onValueChange = {
                email = it
            },

            label = {
                Text("Correo Electrónico")
            },

            leadingIcon = {

                Icon(
                    imageVector =
                        Icons.Default.Email,

                    contentDescription = null,

                    tint = Color.Gray
                )
            },

            singleLine = true,

            enabled = !cargando,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email,

                    imeAction =
                        ImeAction.Next
                ),

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        Color.White,

                    unfocusedTextColor =
                        Color.White,

                    focusedBorderColor =
                        Color(0xFF00F5D4),

                    unfocusedBorderColor =
                        Color.Gray,

                    focusedLabelColor =
                        Color(0xFF00F5D4),

                    unfocusedLabelColor =
                        Color.Gray
                )
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = contrasena,

            onValueChange = {
                contrasena = it
            },

            label = {
                Text("Contraseña")
            },

            leadingIcon = {

                Icon(
                    imageVector =
                        Icons.Default.Lock,

                    contentDescription = null,

                    tint = Color.Gray
                )
            },

            trailingIcon = {

                IconButton(
                    onClick = {
                        mostrarContrasena =
                            !mostrarContrasena
                    }
                ) {

                    Icon(
                        imageVector =
                            if (mostrarContrasena)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,

                        contentDescription =
                            if (mostrarContrasena)
                                "Ocultar contraseña"
                            else
                                "Mostrar contraseña",

                        tint = Color.Gray
                    )
                }
            },

            visualTransformation =
                if (mostrarContrasena)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),

            singleLine = true,

            enabled = !cargando,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password,

                    imeAction =
                        ImeAction.Done
                ),

            keyboardActions =
                KeyboardActions(
                    onDone = {
                        realizarLogin()
                    }
                ),

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        Color.White,

                    unfocusedTextColor =
                        Color.White,

                    focusedBorderColor =
                        Color(0xFF00F5D4),

                    unfocusedBorderColor =
                        Color.Gray,

                    focusedLabelColor =
                        Color(0xFF00F5D4),

                    unfocusedLabelColor =
                        Color.Gray
                )
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        if (cargando) {

            CircularProgressIndicator(
                color = Color(0xFFE94560)
            )

        } else {

            Button(
                onClick = {
                    realizarLogin()
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFE94560)
                    ),

                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Text(
                    "INGRESAR AL PANEL",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}