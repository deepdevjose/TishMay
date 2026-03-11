package com.example.esteticaapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.ui.theme.BackgroundPink
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.TextPrimary
import com.example.esteticaapp.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRegistrationScreen(
    onSaveClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    BackHandler { onBackClick() }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var selectedAvatar by remember { mutableStateOf(Icons.Default.Person) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var expandedSex by remember { mutableStateOf(false) }

    val sexOptions = listOf("Femenino", "Masculino", "Otro")
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()

    val isEmailValid = email.isEmpty() || email.matches(emailRegex)
    val isPasswordValid = password.length >= 6
    val passwordsMatch = password == confirmPassword
    
    val isFormValid = firstName.isNotBlank() && 
                      lastName.isNotBlank() && 
                      age.isNotBlank() && 
                      sex.isNotBlank() && 
                      email.isNotBlank() && 
                      email.matches(emailRegex) &&
                      isPasswordValid &&
                      passwordsMatch

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crea tu Cuenta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PrimaryPink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = BackgroundPink
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Avatar Section
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(PrimaryPink.copy(alpha = 0.1f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = { showAvatarDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(50.dp),
                        tint = PrimaryPink
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(PrimaryPink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Text("Selecciona tu estilo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- SECCIÓN: INFORMACIÓN PERSONAL ---
            item {
                SectionHeader("Información Personal")
            }

            item {
                CustomTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "Nombre(s)",
                    imeAction = ImeAction.Next
                )
            }

            item {
                CustomTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Apellidos",
                    imeAction = ImeAction.Next
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CustomTextField(
                        value = age,
                        onValueChange = { if (it.all { c -> c.isDigit() }) age = it },
                        label = "Edad",
                        modifier = Modifier.weight(0.4f),
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                    
                    Box(modifier = Modifier.weight(0.6f)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedSex,
                            onExpandedChange = { expandedSex = !expandedSex }
                        ) {
                            TextField(
                                value = sex,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sexo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSex) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors()
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

            // --- SECCIÓN: CUENTA ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Cuenta")
            }

            item {
                CustomTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo Electrónico",
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
                    label = "Contraseña",
                    visualTransformation = PasswordVisualTransformation(),
                    imeAction = ImeAction.Next,
                    isError = !isPasswordValid && password.isNotEmpty(),
                    supportingText = if (!isPasswordValid && password.isNotEmpty()) "Mínimo 6 caracteres" else null
                )
            }

            item {
                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirmar Contraseña",
                    visualTransformation = PasswordVisualTransformation(),
                    imeAction = ImeAction.Done,
                    isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                    supportingText = if (!passwordsMatch && confirmPassword.isNotEmpty()) "Las contraseñas no coinciden" else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSaveClick,
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPink,
                        disabledContainerColor = PrimaryPink.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Crear Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAvatarDialog) {
        AvatarSelectorDialog(
            selectedAvatar = selectedAvatar,
            onAvatarSelected = { selectedAvatar = it; showAvatarDialog = false },
            onDismiss = { showAvatarDialog = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp), thickness = 1.dp, color = PrimaryPink.copy(alpha = 0.2f))
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
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    isError: Boolean = false,
    supportingText: String? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
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

@Composable
fun AvatarSelectorDialog(
    selectedAvatar: androidx.compose.ui.graphics.vector.ImageVector,
    onAvatarSelected: (androidx.compose.ui.graphics.vector.ImageVector) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige tu Avatar") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val avatars = listOf(Icons.Default.Person, Icons.Default.Face, Icons.Default.Mood, Icons.Default.AccountCircle)
                avatars.forEach { icon ->
                    IconButton(
                        onClick = { onAvatarSelected(icon) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (selectedAvatar == icon) PrimaryPink.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = PrimaryPink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
