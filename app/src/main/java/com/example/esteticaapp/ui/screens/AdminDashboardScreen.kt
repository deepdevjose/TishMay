package com.example.esteticaapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.esteticaapp.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.example.esteticaapp.NotificationHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val StatusPendingColor = Color(0xFFFFC107)
val StatusAcceptedColor = Color(0xFF4CAF50)
val StatusRejectedColor = Color(0xFFEF5350)
val StatusCompletedColor = Color(0xFF2196F3)
val StatusNoShowColor = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit, onBackToWelcome: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val appointments = remember { mutableStateListOf<Appointment>() }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var currentFilter by remember { mutableStateOf("Todas") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionToConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var actionMessage by remember { mutableStateOf("") }

    val currentDate = remember {
        val sdf = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))
        sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
    
    fun loadAppointments() {
        db.collection("citas")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    appointments.clear()
                    for (doc in snapshot.documents) {
                        val appt = doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                        if (appt != null) appointments.add(appt)
                    }
                }
            }
    }

    LaunchedEffect(Unit) {
        loadAppointments()
    }

    val filteredAppointments = remember(appointments, currentFilter) {
        if (currentFilter == "Todas") appointments else appointments.filter { it.status == currentFilter }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar acción") },
            text = { Text(actionMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        actionToConfirm?.invoke()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Panel de administración", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackToWelcome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar a Inicio", tint = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text("Resumen de Hoy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text(currentDate, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    }
                }

                item {
                    val pendingCount = appointments.count { it.status == "Pendiente" }
                    val acceptedCount = appointments.count { it.status == "Confirmada" }
                    val rejectedCount = appointments.count { it.status == "Cancelada" || it.status == "Rechazada" }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Total hoy", appointments.size.toString(), Icons.Default.CalendarToday, PrimaryPink, Modifier.weight(1f))
                            StatCard("Pendientes", pendingCount.toString(), Icons.Default.PendingActions, StatusPendingColor, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Aceptadas", acceptedCount.toString(), Icons.Default.CheckCircle, StatusAcceptedColor, Modifier.weight(1f))
                            StatCard("Rechazadas", rejectedCount.toString(), Icons.Default.Cancel, StatusRejectedColor, Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Text("Gestión de Citas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val filters = listOf("Todas", "Pendiente", "Confirmada", "Completada")
                        items(filters) { filter ->
                            FilterChip(
                                selected = currentFilter == filter,
                                onClick = { currentFilter = filter },
                                label = { Text(if(filter == "Pendiente") "Pendientes" else if (filter == "Confirmada") "Aceptadas" else filter) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryPink, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                if (filteredAppointments.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventBusy, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No hay citas en este estado", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(filteredAppointments, key = { it.id }) { appointment ->
                        AdminAppointmentCard(
                            appointment = appointment,
                            onAction = { action, msg ->
                                actionMessage = msg
                                actionToConfirm = action
                                showConfirmDialog = true
                            },
                            db = db
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAppointmentCard(appointment: Appointment, onAction: (() -> Unit, String) -> Unit, db: FirebaseFirestore) {
    val context = LocalContext.current
    val rtdb = FirebaseDatabase.getInstance("https://estetica-e0333-default-rtdb.firebaseio.com")

    fun notifyClient(title: String, message: String) {
        if (appointment.userId.isNotEmpty()) {
            rtdb.getReference("client_notifications")
                .child(appointment.userId)
                .push()
                .setValue(mapOf(
                    "title" to title,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false
                ))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(appointment.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(appointment.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Text(appointment.service, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                
                Surface(
                    color = when(appointment.status) {
                        "Confirmada" -> StatusAcceptedColor.copy(alpha = 0.1f)
                        "Completada" -> StatusCompletedColor.copy(alpha = 0.1f)
                        "Cancelada", "Rechazada" -> StatusRejectedColor.copy(alpha = 0.1f)
                        "No Asistió" -> StatusNoShowColor.copy(alpha = 0.1f)
                        else -> StatusPendingColor.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, when(appointment.status) {
                         "Confirmada" -> StatusAcceptedColor
                        "Completada" -> StatusCompletedColor
                        "Cancelada", "Rechazada" -> StatusRejectedColor
                        "No Asistió" -> StatusNoShowColor
                        else -> StatusPendingColor
                    })
                ) {
                    Text(
                        text = appointment.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when(appointment.status) {
                            "Confirmada" -> StatusAcceptedColor
                            "Completada" -> StatusCompletedColor
                            "Cancelada", "Rechazada" -> StatusRejectedColor
                            "No Asistió" -> StatusNoShowColor
                            else -> StatusPendingColor.darken(0.3f)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (appointment.status == "Pendiente") {
                    OutlinedButton(
                        onClick = {
                            onAction({
                                db.collection("citas").document(appointment.id).update("status", "Rechazada")
                                    .addOnSuccessListener { 
                                        notifyClient("Cita Rechazada", "Lo sentimos, tu cita para ${appointment.service} no pudo ser aceptada.")
                                        Toast.makeText(context, "Cita rechazada", Toast.LENGTH_SHORT).show() 
                                    }
                            }, "¿Rechazar cita de ${appointment.clientName}?")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedColor),
                        border = BorderStroke(1.dp, StatusRejectedColor),
                        modifier = Modifier.weight(1f)
                    ) { Text("Rechazar") }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            onAction({
                                db.collection("citas").document(appointment.id).update("status", "Confirmada")
                                    .addOnSuccessListener { 
                                        notifyClient("¡Cita Confirmada!", "Tu cita para ${appointment.service} ha sido aceptada. ¡Te esperamos!")
                                        Toast.makeText(context, "Cita aceptada", Toast.LENGTH_SHORT).show() 
                                    }
                            }, "¿Aceptar cita de ${appointment.clientName}?")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAcceptedColor),
                        modifier = Modifier.weight(1f)
                    ) { Text("Aceptar") }
                } else if (appointment.status == "Confirmada") {
                    OutlinedButton(
                        onClick = {
                             onAction({
                                db.collection("citas").document(appointment.id).update("status", "No Asistió")
                                    .addOnSuccessListener { Toast.makeText(context, "Marcada como No Asistió", Toast.LENGTH_SHORT).show() }
                            }, "¿Marcar como No Asistió?")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusNoShowColor),
                        border = BorderStroke(1.dp, StatusNoShowColor)
                    ) { Text("No Asistió") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                             onAction({
                                db.runTransaction { transaction ->
                                    val apptRef = db.collection("citas").document(appointment.id)
                                    if (appointment.userId.isNotEmpty()) {
                                        val userRef = db.collection("clientes").document(appointment.userId)
                                        transaction.update(userRef, "completedAppointmentsCount", FieldValue.increment(1))
                                    }
                                    transaction.update(apptRef, "status", "Completada")
                                }.addOnSuccessListener {
                                    notifyClient("¡Servicio Finalizado!", "Gracias por confiar en nosotros. Ya puedes calificar tu experiencia.")
                                    Toast.makeText(context, "Servicio completado", Toast.LENGTH_SHORT).show()
                                }
                            }, "¿Completar servicio?")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompletedColor)
                    ) { Text("Completar") }
                } else {
                    Text("Acciones cerradas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

fun Color.darken(factor: Float): Color {
    return Color(
        red = this.red * (1 - factor),
        green = this.green * (1 - factor),
        blue = this.blue * (1 - factor),
        alpha = this.alpha
    )
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
