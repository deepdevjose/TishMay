/*
 * AdminWelcomeScreen es la pantalla inicial del flujo de administracion.
 * Muestra bienvenida y accesos directos para gestionar citas y galeria,
 * permite configurar la capacidad operativa del local por turno,
 * y genera un resumen inteligente de la agenda del dia con proxima cita.
 */

package com.example.esteticaapp.feature.admin.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.esteticaapp.core.model.Appointment
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.SoftRose
import com.example.esteticaapp.ui.theme.TextPrimary
import com.example.esteticaapp.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminWelcomeScreen(onNavigateToDashboard: () -> Unit, onNavigateToGallery: () -> Unit, onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    
    val adminName = auth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Administrador"
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Las notificaciones están desactivadas.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val displayDate = remember {
        val sdf = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))
        sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    val queryDate = remember {
        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
    }

    var showSummary by remember { mutableStateOf(false) }
    var isLoadingSummary by remember { mutableStateOf(false) }
    var showCapacityDialog by remember { mutableStateOf(false) }
    var currentCapacity by remember { mutableIntStateOf(2) }
    
    var countConfirmed by remember { mutableIntStateOf(0) }
    var countPending by remember { mutableIntStateOf(0) }
    var countCancelled by remember { mutableIntStateOf(0) }
    var aiAnalysis by remember { mutableStateOf("") }
    var summaryLastUpdate by remember { mutableStateOf("") }
    var nextAppointmentInfo by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(Unit) {
        db.collection("config").document("appointments").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null && snapshot.exists()) {
                val capacity = (snapshot.get("maxCapacityPerHour") as? Number)?.toInt() ?: 2
                currentCapacity = capacity
            }
        }
    }

    fun generateAISummary() {
        isLoadingSummary = true
        showSummary = true
        
        db.collection("citas")
            .get()
            .addOnSuccessListener { snapshot ->
                val allAppointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                }

                val today = Date()
                val targetDates = mutableSetOf<String>()
                val locales = listOf(Locale.getDefault(), Locale.US, Locale.UK, Locale.forLanguageTag("es-ES"))
                
                locales.forEach { locale ->
                    targetDates.add(SimpleDateFormat("dd MMM, yyyy", locale).format(today))
                    targetDates.add(SimpleDateFormat("dd MMM, yyyy", locale).format(today).replace(".", ""))
                }

                val appointments = allAppointments.filter { appt -> 
                    targetDates.any { target -> appt.date.equals(target, ignoreCase = true) }
                }
                
                countConfirmed = appointments.count { it.status == "Confirmada" || it.status == "Aceptada" || it.status == "Completada" }
                countPending = appointments.count { it.status == "Pendiente" }
                countCancelled = appointments.count { it.status == "Cancelada" || it.status == "No Asistió" || it.status == "Rechazada" }
                
                val maxDailyAppointments = currentCapacity * 4
                val capacityInfo = " Tienes $currentCapacity aplicadoras por sesión, el máximo de citas a atender hoy es de: $maxDailyAppointments."

                aiAnalysis = when {
                    countPending > 0 -> "Detectamos $countPending cita${if(countPending > 1) "s" else ""} pendiente${if(countPending > 1) "s" else ""} por revisar."
                    countConfirmed > 5 -> "Hoy tienes una agenda bastante ocupada con $countConfirmed citas."
                    countConfirmed > 0 -> "Hoy tienes una agenda organizada con $countConfirmed citas."
                    else -> "No hay citas para hoy ($queryDate)."
                } + capacityInfo

                val now = Calendar.getInstance()
                val timeSdf = SimpleDateFormat("hh:mm a", Locale.US)
                
                nextAppointmentInfo = appointments
                    .filter { it.status == "Confirmada" || it.status == "Aceptada" }
                    .filter { appt ->
                        try {
                            val apptTime = Calendar.getInstance().apply {
                                time = timeSdf.parse(appt.time)!!
                                set(Calendar.YEAR, now.get(Calendar.YEAR))
                                set(Calendar.MONTH, now.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                            }
                            apptTime.after(now)
                        } catch (e: Exception) { false }
                    }
                    .minByOrNull { appt ->
                        try { timeSdf.parse(appt.time)?.time ?: Long.MAX_VALUE } catch (e: Exception) { Long.MAX_VALUE }
                    }

                summaryLastUpdate = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                isLoadingSummary = false
            }
            .addOnFailureListener {
                isLoadingSummary = false
            }
    }

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TextButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).zIndex(1f), 
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cerrar Sesión", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                Text(text = "Hola, $adminName", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 34.sp))
                Text(text = displayDate, style = MaterialTheme.typography.labelLarge.copy(color = PrimaryPink, fontWeight = FontWeight.Medium), modifier = Modifier.padding(top = 8.dp))
                
                Spacer(modifier = Modifier.height(40.dp))
                
                WelcomeCard(title = "Gestionar citas", subtitle = "Aceptar, rechazar y completar citas", icon = Icons.AutoMirrored.Filled.ArrowForward, isPrimary = true, onClick = onNavigateToDashboard)
                
                Spacer(modifier = Modifier.height(16.dp))

                WelcomeCard(
                    title = "Capacidad del local", 
                    subtitle = "Configurar aplicadoras por turno", 
                    icon = Icons.Default.Groups, 
                    isPrimary = false, 
                    isIA = true,
                    onClick = { showCapacityDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                WelcomeCard(
                    title = "Gestionar Galería", 
                    subtitle = "Edita fotos y descripciones", 
                    icon = Icons.Default.Collections, 
                    isPrimary = false, 
                    isIA = true,
                    onClick = onNavigateToGallery
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                WelcomeCard(title = "Resumen inteligente", subtitle = "Estado de la agenda en segundos", icon = Icons.Default.Psychology, isPrimary = false, isIA = true, onClick = { generateAISummary() })
                
                AnimatedVisibility(
                    visible = showSummary, 
                    enter = expandVertically() + fadeIn(), 
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 40.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9FA)),
                        border = if(!isLoadingSummary) BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.2f)) else null
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Psychology, null, tint = PrimaryPink, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Asistente AI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                Spacer(modifier = Modifier.weight(1f))
                                if (!isLoadingSummary) {
                                    IconButton(onClick = { showSummary = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, "Ocultar", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            
                            if (isLoadingSummary) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                                    CircularProgressIndicator(color = PrimaryPink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    SummaryStat("Confirmadas", countConfirmed.toString(), Color(0xFF4CAF50), Icons.Default.CheckCircle)
                                    SummaryStat("Pendientes", countPending.toString(), Color(0xFFFF9800), Icons.Default.PendingActions)
                                    SummaryStat("Canceladas", countCancelled.toString(), Color(0xFFF44336), Icons.Default.Remove)
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = PrimaryPink.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(text = aiAnalysis, style = MaterialTheme.typography.bodyLarge, lineHeight = 22.sp)
                                
                                nextAppointmentInfo?.let { appt ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.1f))) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Schedule, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(text = "PRÓXIMA CITA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)))
                                                Text(text = "${appt.clientName} • ${appt.time}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showCapacityDialog) {
        var tempCapacity by remember { mutableIntStateOf(currentCapacity) }
        
        AlertDialog(
            onDismissRequest = { showCapacityDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = PrimaryPink)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Configurar Capacidad", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column {
                    Text(
                        "Define el número de aplicadoras disponibles por cada turno de 2 horas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if(tempCapacity > 1) tempCapacity-- },
                            modifier = Modifier.background(SoftRose, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, null, tint = PrimaryPink)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                            Text(
                                text = tempCapacity.toString(),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text("Aplicadoras", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        
                        IconButton(
                            onClick = { tempCapacity++ },
                            modifier = Modifier.background(SoftRose, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, null, tint = PrimaryPink)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Surface(
                        color = PrimaryPink.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Con $tempCapacity aplicadoras, puedes atender hasta ${tempCapacity * 4} citas por día.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("config").document("appointments")
                            .set(mapOf("maxCapacityPerHour" to tempCapacity))
                            .addOnSuccessListener {
                                showCapacityDialog = false
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) {
                    Text("Actualizar Capacidad", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
    }
}

@Composable
fun WelcomeCard(title: String, subtitle: String, icon: ImageVector, isPrimary: Boolean, isIA: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick, 
        modifier = Modifier.fillMaxWidth().height(120.dp), 
        shape = RoundedCornerShape(24.dp), 
        elevation = CardDefaults.cardElevation(defaultElevation = if(isPrimary) 6.dp else 1.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(if (isPrimary) Brush.horizontalGradient(colors = listOf(Color.White, Color(0xFFFFF5F8))) else if (isIA) Brush.linearGradient(colors = listOf(Color(0xFFFFF9FA), Color.White)) else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))).padding(24.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                }
                Box(modifier = Modifier.size(52.dp).background(if(isPrimary) PrimaryPink else PrimaryPink.copy(alpha = 0.1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, null, tint = if(isPrimary) Color.White else PrimaryPink, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
