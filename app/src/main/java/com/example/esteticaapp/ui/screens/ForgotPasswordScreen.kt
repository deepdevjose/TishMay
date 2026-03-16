package com.example.esteticaapp.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.Dimensions
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.TextPrimary
import com.example.esteticaapp.ui.theme.TextSecondary
import com.example.esteticaapp.ui.theme.TitleGrey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    initialEmail: String = "",
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var isEmailSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Estados de validación
    var emailError by remember { mutableStateOf(false) }
    var emailErrorMessage by remember { mutableStateOf<String?>(null) }
    var sentEmailAddress by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Feedback visual para reenvío
    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // Helper para enmascarar email
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        
        val maskedName = if (name.length > 3) {
            name.take(3) + "*".repeat(name.length - 3)
        } else {
            name + "*"
        }
        return "$maskedName@$domain"
    }

    // Validación de email
    fun validateEmail(input: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }

    fun handleSendEmail() {
        if (email.isBlank()) {
            emailError = true
            emailErrorMessage = "Ingresa tu correo electrónico."
            return
        }
        
        if (!validateEmail(email)) {
            emailError = true
            emailErrorMessage = "Ingresa un correo electrónico válido."
            return
        }

        isLoading = true
        emailError = false
        emailErrorMessage = null
        keyboardController?.hide()

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    sentEmailAddress = email
                    isEmailSent = true
                } else {
                    // Manejo de errores traducido
                    emailError = true
                    emailErrorMessage = getFirebaseErrorMessage(task.exception)
                }
            }
    }
    
    // Validar si el botón debe estar habilitado
    val isButtonEnabled = email.isNotBlank() && !isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPink
                )
            )
        },
        containerColor = BackgroundPink
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Cambiar Arrangement.Center a Top para controlar mejor el espaciado superior
            verticalArrangement = Arrangement.Top
        ) {
            
            if (!isEmailSent) {
                // Espaciado superior ajustado (aprox 10-15% de la pantalla, o un valor fijo razonable)
                Spacer(modifier = Modifier.height(48.dp))

                // 6. Icono mejorado
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(4.dp, shape = CircleShape)
                        .background(PrimaryPink, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Restablecer Contraseña",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TitleGrey,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Texto descriptivo mejorado
                Text(
                    text = "Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Input con estados
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        // 6. Validación en tiempo real (limpia error)
                        if (emailError) {
                            emailError = false
                            emailErrorMessage = null
                        }
                    },
                    label = { 
                        Text(
                            text = "Correo Electrónico",
                            style = MaterialTheme.typography.bodyMedium // Asegurar consistencia en tamaño
                        ) 
                    },
                    placeholder = { Text("ejemplo@email.com", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color(0xFFF0F0F0),
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = Color(0xFFE7D7DA),
                        errorBorderColor = Color(0xFFB00020), // Rojo con más contraste
                        focusedLabelColor = PrimaryPink,
                        errorLabelColor = Color(0xFFB00020), // Rojo con más contraste
                        cursorColor = PrimaryPink
                    ),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { 
                            if (isButtonEnabled) handleSendEmail() 
                        }
                    )
                )

                // Mensaje de error debajo del input
                AnimatedVisibility(visible = emailError && emailErrorMessage != null) {
                    Text(
                        text = emailErrorMessage ?: "",
                        color = Color(0xFFB00020), // Rojo con más contraste
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { handleSendEmail() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPink,
                        disabledContainerColor = Color(0xFFE0E0E0), // Gris claro disabled
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF9E9E9E) // Texto gris oscuro disabled
                    ),
                    enabled = isButtonEnabled // Desactivar si vacío o loading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Enviando...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        // 1. Texto del botón ajustado
                        Text(
                            text = "Enviar enlace",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                // Espaciado superior reducido para subir el contenido (Mejora 1)
                Spacer(modifier = Modifier.height(32.dp))

                // Email Sent State
                // Icono con más peso visual (Mejora 2)
                Box(
                    modifier = Modifier
                        .size(100.dp) // Aumentado de 80.dp -> 100.dp (aprox 20-25%)
                        .background(Color(0xFFE8F5E9), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp), // Aumentado de 40.dp -> 50.dp
                        tint = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Texto principal más informativo (Mejora 3)
                Text(
                    text = "Enlace enviado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TitleGrey,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card compacta (Mejora 4)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), // Padding interno reducido
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enviamos un enlace de restablecimiento a:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp)) // Espacio reducido
                        
                        // Masked email
                        Text(
                            text = maskEmail(sentEmailAddress),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp)) // Espacio reducido
                        
                        // Microcopy mejorado (Mejora 7)
                        Text(
                            text = "Si no lo ves, revisa spam o correo no deseado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp)) // Separación aumentada para destacar CTA (Mejora 5)

                // 5. Botón abrir correo (CTA Principal)
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_EMAIL)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Abrir correo"))
                        } catch (_: Exception) {
                           showToast("No se encontró una aplicación de correo.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) {
                    Text(
                        text = "Abrir correo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // 6. Jerarquía de acciones
                
                // Acción Secundaria: Reenviar enlace
                Text(
                    text = buildAnnotatedString {
                        append("¿No recibiste el correo? ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryPink)) {
                            append("Reenviar enlace")
                        }
                    },
                    modifier = Modifier
                        .clickable(enabled = !isLoading) { 
                             isLoading = true
                             FirebaseAuth.getInstance().sendPasswordResetEmail(sentEmailAddress)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        showToast("Enlace reenviado con éxito")
                                    } else {
                                        showToast("Error al reenviar: ${task.exception?.localizedMessage}")
                                    }
                                }
                        }
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Acción Terciaria: Volver al inicio (más discreto)
                TextButton(
                    onClick = onBackToLogin,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text(
                        text = "Volver al inicio de sesión",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Helper para traducir errores de Firebase logicamente
private fun getFirebaseErrorMessage(e: Exception?): String {
    val message = e?.localizedMessage?.lowercase() ?: ""
    return when {
        e is FirebaseAuthInvalidUserException || message.contains("no user record") ->
            "No existe una cuenta con este correo."
        message.contains("badly formatted") || message.contains("invalid email") ->
            "Ingresa un correo electrónico válido."
        message.contains("network") || message.contains("connection") || message.contains("host") ->
            "No hay conexión a internet. Verifica tu red."
        message.contains("too many requests") ->
            "Demasiados intentos. Intenta más tarde."
        else -> "Error al enviar el correo. Intenta nuevamente."
    }
}
