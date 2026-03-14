package com.example.esteticaapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esteticaapp.AdminConfig
import com.example.esteticaapp.LoginState
import com.example.esteticaapp.LoginViewModel
import com.example.esteticaapp.R
import com.example.esteticaapp.ui.theme.Dimensions
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.TextPrimary
import com.example.esteticaapp.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: (String) -> Unit,
    onNavigateTo: (String) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoadingEmail by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Estados de validación
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val viewModel: LoginViewModel = viewModel()
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Estado global de carga para bloquear UI
    val isAnyLoading = isLoadingEmail || loginState is LoginState.Loading

    // Estados de animación
    var isScreenVisible by remember { mutableStateOf(false) }
    
    // Efecto de entrada animada
    LaunchedEffect(Unit) {
        isScreenVisible = true
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                val destination = (loginState as LoginState.Success).navigateTo
                // Si LoginScreen está embebido en MainActivity usando el estado 'currentScreen',
                // necesitamos una forma de comunicarle el cambio de pantalla al padre.
                // Sin embargo, LoginScreen recibe 'onNavigateTo'.
                onNavigateTo(destination)
                viewModel.resetState()
            }
            is LoginState.Error -> {
                // Microdetalle 12: Haptic feedback al fallar
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                errorMessage = (loginState as LoginState.Error).message
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Función auxiliar para login
    fun performLogin() {
        if (email.isNotBlank() && password.isNotBlank()) {
            isLoadingEmail = true
            errorMessage = null
            keyboardController?.hide()
            
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    coroutineScope.launch {
                        isLoadingEmail = false
                        if (AdminConfig.isAdmin(email)) {
                            onNavigateTo("admin_dashboard")
                        } else {
                            onLoginSuccess(email) 
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Haptic feedback también aquí por si acaso
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    isLoadingEmail = false
                    errorMessage = "Correo o contraseña incorrectos" // Mejora 9: Mensaje limpio sin "Error:"
                }
        } else {
             // Validación de campos vacíos manejada en UI individual o aquí
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Fondo blanco puro
            .padding(horizontal = Dimensions.PaddingExtraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 8. Animación de entrada: Logo e Intro (Fade In)
            AnimatedVisibility(
                visible = isScreenVisible,
                enter = fadeIn(animationSpec = tween(200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 1. Reducir peso del branding
                    Text(
                        text = "TISHMAY",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp, // Reducido ~15% (antes Dimensions.TextSizeDisplay podría ser mayor)
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = PrimaryPink
                    )
                    Text(
                        text = "ESTÉTICA PROFESIONAL",
                        style = MaterialTheme.typography.labelMedium, // Reducido tamaño
                        color = TextSecondary,
                        letterSpacing = 1.0.sp
                    )
                    
                    Spacer(modifier = Modifier.height(Dimensions.SpacerMedium))
                    
                    // 2. Frase de bienvenida
                    Text(
                        text = "Bienvenida de nuevo",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacerMedium))

            // 9. Error de Login Global (Arriba)
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFE15A5A),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = Dimensions.SpacerSmall)
                )
            }

            // 8. Animación de entrada: Inputs (Slide Up + Fade In)
            AnimatedVisibility(
                visible = isScreenVisible,
                enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250))
            ) {
                Column {
                    // 3. Mejorar campos de texto (Email)
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = false
                            errorMessage = null
                        },
                        label = { Text("Correo Electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnyLoading,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = emailError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color(0xFFF0F0F0),
                            focusedBorderColor = Color(0xFFE8739B),
                            unfocusedBorderColor = Color(0xFFE7D7DA),
                            errorBorderColor = Color(0xFFE15A5A),
                            errorLabelColor = Color(0xFFE15A5A),
                            focusedLabelColor = Color(0xFFE8739B),
                            cursorColor = Color(0xFFE8739B),
                            errorCursorColor = Color(0xFFE15A5A)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    // Error de campo debajo
                    if (emailError) {
                        Text(
                            text = "El correo es obligatorio",
                            color = Color(0xFFE15A5A),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimensions.SpacerMedium))

                    // 3. Mejorar campos de texto (Password)
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = false 
                            errorMessage = null
                        },
                        label = { Text("Contraseña") },
                        enabled = !isAnyLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isAnyLoading) {
                                Icon(imageVector = image, contentDescription = null, tint = if(isAnyLoading) Color.Gray else TextSecondary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = passwordError,
                         colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color(0xFFF0F0F0),
                            focusedBorderColor = Color(0xFFE8739B),
                            unfocusedBorderColor = Color(0xFFE7D7DA),
                            errorBorderColor = Color(0xFFE15A5A),
                            errorLabelColor = Color(0xFFE15A5A),
                            focusedLabelColor = Color(0xFFE8739B),
                            cursorColor = Color(0xFFE8739B),
                            errorCursorColor = Color(0xFFE15A5A)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done // 10. UX Teclado Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (email.isBlank()) emailError = true
                                if (password.isBlank()) passwordError = true
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    performLogin()
                                }
                            }
                        )
                    )
                    // Error de campo debajo
                    if (passwordError) {
                        Text(
                            text = "La contraseña es obligatoria",
                            color = Color(0xFFE15A5A),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimensions.SpacerMedium))

                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable(enabled = !isAnyLoading) { onForgotPasswordClick(email) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacerExtraLarge))

            // 8. Animación de entrada: Botones (Fade In delay)
            AnimatedVisibility(
                visible = isScreenVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 100))
            ) {
                Column {
                    // 4. Mejorar botón principal
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.97f else 1f,
                        animationSpec = tween(120), 
                        label = "buttonScale"
                    )

                    Button(
                        onClick = {
                            var hasError = false
                            if (email.isBlank()) { emailError = true; hasError = true } else emailError = false
                            if (password.isBlank()) { passwordError = true; hasError = true } else passwordError = false
                            
                            if (!hasError) {
                                performLogin()
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        enabled = !isAnyLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp) // Altura standard
                            .scale(scale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    },
                                    onTap = {
                                         // La acción onClick maneja la lógica, esto es solo visual
                                    }
                                )
                            },
                        shape = RoundedCornerShape(24.dp), // Radio 20-24dp
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPink,
                            disabledContainerColor = PrimaryPink.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp, // Sombra casi inexistente
                            pressedElevation = 0.dp
                        )
                    ) {
                        // 11. Loading state
                        if (isLoadingEmail || loginState is LoginState.Loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.White, 
                                    modifier = Modifier.size(20.dp), 
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Ingresando...", style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Text(
                                text = "Iniciar Sesión",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimensions.SpacerLarge))

                    // 6. Mejorar el divisor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFDDDDDD), thickness = 1.dp)
                        Text(
                            text = "o continúa con", // Minuscula
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9C8D91),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFDDDDDD), thickness = 1.dp)
                    }

                    Spacer(modifier = Modifier.height(Dimensions.SpacerLarge))

                    // 5. Mejorar botón de Google
                    OutlinedButton(
                        onClick = { viewModel.signInWithGoogle(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(24.dp), // Consistente con botón principal
                        border = BorderStroke(1.dp, Color(0xFFE6D9DD)), // Borde suave
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = TextPrimary
                        ),
                        enabled = !isAnyLoading
                    ) {
                         if (loginState is LoginState.Loading) {
                             // Si estamos cargando GoogleSign podría mostrarse aquí también un loading si ese estado fuera especifico
                             // Pero por simplicidad mantenemos el loading general si bloquea todo
                             CircularProgressIndicator(
                                color = PrimaryPink, 
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                         } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google), // Usando el SVG convertido
                                    contentDescription = "Google Logo",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continuar con Google", // Texto actualizado
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Dimensions.SpacerExtraLarge))

                    // 7. Cambiar texto de registro
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         Text(
                            text = buildAnnotatedString {
                                append("¿No tienes cuenta? ")
                                withStyle(style = SpanStyle(
                                    color = PrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append("Regístrate")
                                }
                            },
                            modifier = Modifier.clickable(enabled = !isAnyLoading) { onRegisterClick() },
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
