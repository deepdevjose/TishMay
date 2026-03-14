package com.example.esteticaapp.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.Dimensions
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.TextPrimary
import com.example.esteticaapp.ui.theme.TextSecondary
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRegistrationScreen(
    onSaveSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
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
    var selectedAvatar by remember { mutableStateOf(Icons.Default.Person) }

    // Datos de la Cuenta
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var expandedSex by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sexOptions = listOf("Femenino", "Masculino", "Prefiero no decirlo")
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()

    // Validaciones
    val isEmailValid = email.isEmpty() || email.matches(emailRegex)
    val isPasswordValid = password.length >= 6
    val passwordsMatch = password == confirmPassword
    
    val isStep1Valid = email.isNotBlank() && isEmailValid && 
                       password.isNotBlank() && isPasswordValid &&
                       passwordsMatch

    val isStep2Valid = firstName.isNotBlank() && 
                       lastName.isNotBlank() && 
                       age.isNotBlank() && 
                       sex.isNotBlank()

    // DatePicker State
    val datePickerState = rememberDatePickerState()

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
                        
                        // Calcular edad
                        val today = Calendar.getInstance()
                        var calculatedAge = today.get(Calendar.YEAR) - year
                        if (today.get(Calendar.DAY_OF_YEAR) < calendar.get(Calendar.DAY_OF_YEAR)) {
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
            DatePicker(state = datePickerState)
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
            // Indicador de pasos simple
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (step >= 1) PrimaryPink else Color.LightGray))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (step >= 2) PrimaryPink else Color.LightGray))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacerMedium)
            ) {
                if (errorMessage != null) {
                    item {
                        Text(text = errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (step == 1) {
                    // --- PASO 1: CUENTA ---
                    item {
                        CustomTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Correo Electrónico *",
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            isError = !isEmailValid && email.isNotEmpty(),
                            supportingText = if (!isEmailValid && email.isNotEmpty()) "Formato inválido" else null
                        )
                    }

                    item {
                        CustomTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Contraseña *",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            imeAction = ImeAction.Next,
                            isError = !isPasswordValid && password.isNotEmpty(),
                            supportingText = "Mínimo 6 caracteres", // Texto de ayuda visible
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
                    // --- PASO 2: PERFIL ---
                    item {
                        Spacer(modifier = Modifier.height(Dimensions.SpacerSmall))
                        // Avatar Section
                        Box(
                            modifier = Modifier
                                .size(100.dp)
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
                                imageVector = selectedAvatar,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(60.dp),
                                tint = PrimaryPink
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .background(PrimaryPink, CircleShape)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.White), // Borde blanco
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp).background(PrimaryPink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Elige tu avatar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = TextPrimary) // Texto mejorado
                        Spacer(modifier = Modifier.height(24.dp)) // Espaciado aumentado
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
                            // Date Picker Field
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
                        }
                    }
                    
                    // Fixed Layout for Sex and dummy place (wait, previous code had duplications)
                     item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reusing the sex selection logic
                            Box(modifier = Modifier.weight(1f)) {
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

            Spacer(modifier = Modifier.height(Dimensions.SpacerMedium))

            Button(
                onClick = {
                    if (step == 1) {
                        step = 2
                    } else {
                        // Create Account Logic
                        isLoading = true
                        errorMessage = null
                        val auth = FirebaseAuth.getInstance()
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user
                                user?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        val userData = hashMapOf(
                                            "uid" to user.uid,
                                            "firstName" to firstName,
                                            "lastName" to lastName,
                                            "age" to age,
                                            "sex" to sex,
                                            "email" to email,
                                            "role" to "client",
                                            "cancellationCount" to 0
                                        )
                                        FirebaseFirestore.getInstance().collection("clientes")
                                            .document(user.uid)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                onSaveSuccess()
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "Error guardando datos: ${e.localizedMessage}"
                                            }
                                    }
                                    ?.addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = "Error enviando verificación: ${e.localizedMessage}"
                                    }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = "Error en registro: ${e.localizedMessage}"
                            }
                    }
                },
                enabled = (if (step == 1) isStep1Valid else isStep2Valid) && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Altura más estándar
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPink,
                    disabledContainerColor = Color(0xFFFFD1DC), // Rosa muy claro
                    disabledContentColor = Color.Gray // Texto gris
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        if (step == 1) "Siguiente" else "Crear Perfil", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimensions.SpacerExtraLarge))
        }
    }

    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = rememberModalBottomSheetState()
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
                
                val avatars = listOf(
                    Icons.Default.Person, Icons.Default.Face, Icons.Default.Mood, Icons.Default.AccountCircle,
                    Icons.Default.EmojiPeople, Icons.Default.SportsGymnastics, Icons.Default.AccessibilityNew, Icons.Default.SelfImprovement
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatars) { icon ->
                        val isSelected = selectedAvatar == icon
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryPink.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { 
                                    selectedAvatar = icon
                                    // Optional: close on selection or keep open? User didn't specify, but "Listo" button suggested.
                                    // Let's keep open and add "Listo" button or just select.
                                    // User said "Al tocar... avatar seleccionado... boton opcional: Listo"
                                }
                                .then(if (isSelected) Modifier.padding(4.dp) else Modifier), // Margin for border effect
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
    trailingIcon: @Composable (() -> Unit)? = null
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
        colors = textFieldColors()
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

