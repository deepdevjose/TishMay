package com.example.esteticaapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.Dimensions
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.Calendar

// Agregar mapa de avatares disponibles globalmente para reutilizar
val predefinedAvatars = listOf(
    "face_3" to Icons.Default.Face3,      // Mujer
    "face_4" to Icons.Default.Face4,      // Mujer
    "woman" to Icons.Default.Woman,       // Mujer
    "person" to Icons.Default.Person,     // Neutro
    "face" to Icons.Default.Face,         // Neutro/Hombre
    "man" to Icons.Default.Man,           // Hombre
    "spa" to Icons.Default.Spa,           // Estética
    "self_improvement" to Icons.Default.SelfImprovement,
    "emoji_people" to Icons.Default.EmojiPeople
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRegistrationScreen(
    initialEmail: String = "",
    onSaveSuccess: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    // Verificamos si es un usuario que viene de Google
    val isGoogleUser = currentUser != null

    var step by remember { mutableIntStateOf(1) } // 1: Cuenta, 2: Perfil

    BackHandler { 
        if (step > 1) step-- else onBackClick()
    }

    // Datos del Perfil
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDateDisplay by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") } // Se calcula automáticamente
    var sex by remember { mutableStateOf("") }
    
    // Avatar seleccionado (guardamos la clave "string", no el ícono)
    var selectedAvatarKey by remember { mutableStateOf("person") }
    // Helper para obtener el ícono visual
    val selectedAvatarIcon = predefinedAvatars.find { it.first == selectedAvatarKey }?.second ?: Icons.Default.Person

    // Datos de la Cuenta
    var email by remember { mutableStateOf(currentUser?.email ?: initialEmail) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var expandedSex by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Pre-llenar datos si vienen de Google
    LaunchedEffect(currentUser) {
        if (currentUser != null && firstName.isEmpty() && lastName.isEmpty()) {
            val nameParts = currentUser.displayName?.split(" ")
            firstName = nameParts?.firstOrNull() ?: ""
            lastName = nameParts?.drop(1)?.joinToString(" ") ?: ""
        }
    }

    val sexOptions = listOf("Femenino", "Masculino", "Prefiero no decirlo", "Otro")
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()

    // Validaciones
    val isEmailValid = email.isEmpty() || email.matches(emailRegex)
    val isPasswordValid = password.length >= 8 // Mejor seguridad (8 chars)
    val passwordsMatch = password == confirmPassword
    
    val isStep1Valid = email.isNotBlank() && isEmailValid && 
                       password.isNotBlank() && isPasswordValid &&
                       passwordsMatch

    val isStep2Valid = firstName.isNotBlank() && 
                       lastName.isNotBlank() && 
                       age.isNotBlank() && 
                       sex.isNotBlank()

    // DatePicker State con validación de edad
    val maxDateMillis = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.YEAR, -13)
        c.timeInMillis
    }
    val minDateMillis = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.YEAR, -100)
        c.timeInMillis
    }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= maxDateMillis && utcTimeMillis >= minDateMillis
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = millis
                        
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        val month = calendar.get(Calendar.MONTH) + 1
                        val year = calendar.get(Calendar.YEAR)
                        birthDateDisplay = "$day/$month/$year" 
                        
                        val current = Calendar.getInstance()
                        var calculatedAge = current.get(Calendar.YEAR) - year
                        if (current.get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
                            calculatedAge--
                        }
                        age = calculatedAge.toString()
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Seleccionar fecha", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (step == 1) "Crea tu Cuenta" else "Completa tu Perfil", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onBackClick() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Volver", 
                            tint = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = BackgroundPink
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimensions.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier
                    .size(if (step == 1) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (step >= 1) PrimaryPink else Color.LightGray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(20.dp).height(2.dp).background(if (step >= 2) PrimaryPink else Color.LightGray))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .size(if (step == 2) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (step >= 2) PrimaryPink else Color.LightGray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (step == 1) "Paso 1: Cuenta" else "Paso 2: Perfil",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                modifier = Modifier.weight(1f),
                label = "stepAnimation"
            ) { currentStep ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacerMedium)
                ) {
                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage!!, 
                                color = Color.Red, 
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (currentStep == 1) {
                        item {
                            CustomTextField(
                                value = email,
                                onValueChange = { if (!isGoogleUser) email = it },
                                label = "Correo Electrónico *",
                                enabled = !isGoogleUser && !isLoading,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                isError = !isEmailValid && email.isNotEmpty(),
                                supportingText = if (isGoogleUser) "Correo vinculado a Google" else if (!isEmailValid && email.isNotEmpty()) "Ingresa un correo electrónico válido" else null,
                                trailingIcon = {
                                    if (isGoogleUser) {
                                        Icon(Icons.Default.Verified, contentDescription = "Verificado", tint = PrimaryPink)
                                    }
                                }
                            )
                        }

                        item {
                            CustomTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = if (isGoogleUser) "Crea una contraseña para tu cuenta *" else "Contraseña *",
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                imeAction = ImeAction.Next,
                                isError = !isPasswordValid && password.isNotEmpty(),
                                supportingText = "Mínimo 8 caracteres", 
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                            )
                        }

                        item {
                            CustomTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Confirmar Contraseña *",
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                imeAction = ImeAction.Done,
                                isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                                supportingText = if (!passwordsMatch && confirmPassword.isNotEmpty()) "Las contraseñas no coinciden" else null,
                                trailingIcon = {
                                    val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(imageVector = image, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            Spacer(modifier = Modifier.height(Dimensions.SpacerSmall))
                            Box(modifier = Modifier.size(100.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(PrimaryPink.copy(alpha = 0.1f))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(),
                                            onClick = { showAvatarSheet = true }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedAvatarIcon,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.size(60.dp),
                                        tint = PrimaryPink
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp)
                                        .border(2.dp, Color.White, CircleShape)
                                        .background(PrimaryPink, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { showAvatarSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Toca para elegir un avatar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item {
                            CustomTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = "Nombre *",
                                imeAction = ImeAction.Next
                            )
                        }

                        item {
                            CustomTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = "Apellido(s) *",
                                imeAction = ImeAction.Next
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(0.5f)) {
                                    CustomTextField(
                                        value = birthDateDisplay,
                                        onValueChange = {},
                                        label = "F. Nacimiento *",
                                        trailingIcon = {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryPink)
                                        }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { showDatePicker = true }
                                    )
                                }
                                
                                Box(modifier = Modifier.weight(0.5f)) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedSex,
                                        onExpandedChange = { expandedSex = !expandedSex }
                                    ) {
                                        CustomTextField(
                                            value = sex,
                                            onValueChange = {},
                                            label = "Género *",
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSex) },
                                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedSex,
                                            onDismissRequest = { expandedSex = false }
                                        ) {
                                            sexOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = { sex = option; expandedSex = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacerSmall))

            val isButtonEnabled = (if (step == 1) isStep1Valid else isStep2Valid) && !isLoading

            Button(
                onClick = {
                    if (step == 1) {
                        isLoading = true
                        errorMessage = null
                        
                        if (isGoogleUser && currentUser?.email?.lowercase() == email.lowercase().trim()) {
                            // VINCULACIÓN: El usuario ya está autenticado con Google.
                            // Le vinculamos el método de contraseña para que tenga ambas opciones.
                            val credential = EmailAuthProvider.getCredential(email, password)
                            currentUser.linkWithCredential(credential)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    // Si tiene éxito o ya estaba vinculado, avanzamos
                                    if (task.isSuccessful || task.exception?.message?.contains("provider already linked") == true) {
                                        step = 2
                                    } else {
                                        errorMessage = "Error al vincular: ${getFirebaseErrorMessage(task.exception)}"
                                    }
                                }
                        } else {
                            // Flujo normal: Registro manual desde cero
                            auth.fetchSignInMethodsForEmail(email)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val methods = task.result?.signInMethods
                                        if (!methods.isNullOrEmpty()) {
                                            if (methods.contains("google.com")) {
                                                errorMessage = "Este correo ya está en uso con Google. Por favor, inicia sesión con Google primero."
                                            } else {
                                                errorMessage = "Este correo ya está registrado. Intenta iniciar sesión."
                                            }
                                        } else {
                                            step = 2
                                        }
                                    } else {
                                        errorMessage = getFirebaseErrorMessage(task.exception)
                                    }
                                }
                        }
                    } else {
                        // PASO 2: Guardado de perfil
                        isLoading = true
                        errorMessage = null
                        val user = auth.currentUser
                        if (user != null) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName("$firstName $lastName")
                                .build()
                            
                            user.updateProfile(profileUpdates).addOnCompleteListener { 
                                // Si no está verificado (es cuenta nueva de email), enviamos verificación
                                if (!user.isEmailVerified) {
                                    user.sendEmailVerification()
                                }
                                
                                saveProfileToFirestore(user.uid, firstName, lastName, age, sex, email, selectedAvatarKey, onSaveSuccess) { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            }
                        }
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPink,
                    disabledContainerColor = Color(0xFFFFD1DC),
                    disabledContentColor = Color.Gray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Procesando...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(
                        if (step == 1) "Siguiente" else "Crear Perfil", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!isButtonEnabled && step == 2 && !isLoading && (firstName.isBlank() || lastName.isBlank() || age.isBlank() || sex.isBlank())) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Completa todos los campos obligatorios", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(Dimensions.SpacerExtraLarge))
        }
    }

    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Elige tu avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(predefinedAvatars) { (key, icon) ->
                        val isSelected = selectedAvatarKey == key
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryPink.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { selectedAvatarKey = key }
                                .then(if (isSelected) Modifier.padding(4.dp) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                             if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .border(2.dp, PrimaryPink, CircleShape)
                                )
                             }
                             Icon(
                                 imageVector = icon, 
                                 contentDescription = null, 
                                 modifier = Modifier.size(40.dp), 
                                 tint = if (isSelected) PrimaryPink else Color.Gray
                             )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { showAvatarSheet = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun saveProfileToFirestore(
    uid: String,
    firstName: String,
    lastName: String,
    age: String,
    sex: String,
    email: String,
    avatar: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val userData = hashMapOf(
        "uid" to uid,
        "firstName" to firstName,
        "lastName" to lastName,
        "age" to age,
        "sex" to sex,
        "email" to email,
        "avatar" to avatar,
        "role" to "client",
        "cancellationCount" to 0
    )
    FirebaseFirestore.getInstance().collection("clientes")
        .document(uid)
        .set(userData)
        .addOnSuccessListener {
            onSuccess(email)
        }
        .addOnFailureListener { e ->
            onError("Error guardando datos: ${getFirebaseErrorMessage(e)}")
        }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        colors = textFieldColors(),
        enabled = enabled
    )
}

@Composable
fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedIndicatorColor = PrimaryPink,
    unfocusedIndicatorColor = Color.Transparent,
    errorContainerColor = Color.White
)

private fun getFirebaseErrorMessage(e: Exception?): String {
    val message = e?.localizedMessage?.lowercase() ?: ""
    return when {
        e is FirebaseAuthUserCollisionException || message.contains("email already in use") ->
            "Este correo ya está registrado. Intenta iniciar sesión."
        e is FirebaseAuthInvalidCredentialsException || message.contains("invalid") ->
            "El formato del correo es incorrecto."
        e is FirebaseAuthWeakPasswordException || message.contains("weak password") ->
            "La contraseña debe tener al menos 6 caracteres."
        message.contains("network") || message.contains("connection") || message.contains("host") ->
            "No hay conexión a internet. Verifica tu red."
        message.contains("too many requests") ->
            "Demasiados intentos. Por favor espera unos minutos."
        else -> "Ha ocurrido un error. Intenta nuevamente."
    }
}
