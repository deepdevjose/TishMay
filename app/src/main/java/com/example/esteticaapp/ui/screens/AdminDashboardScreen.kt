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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.esteticaapp.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.example.esteticaapp.NotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Colores semánticos
val StatusPendingColor = Color(0xFFFFC107) // Amber
val StatusAcceptedColor = Color(0xFF4CAF50) // Green
val StatusRejectedColor = Color(0xFFEF5350) // Red
val StatusCompletedColor = Color(0xFF2196F3) // Blue
val StatusNoShowColor = Color(0xFFFF9800) // Orange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit, onBackToWelcome: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val rtdb = FirebaseDatabase.getInstance("https://estetica-e0333-default-rtdb.firebaseio.com")
    val context = androidx.compose.ui.platform.LocalContext.current
    val appointments = remember { mutableStateListOf<Appointment>() }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Estado para filtros
    var currentFilter by remember { mutableStateOf("Todas") } // Todas, Pendientes, Confirmada, Completada
    
    // Estado para diálogo de confirmación
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionToConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var actionMessage by remember { mutableStateOf("") }

    // Fecha actual formateada
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

    DisposableEffect(Unit) {
        // Escuchar notificaciones desde Realtime Database
        val notifRef = rtdb.getReference("admin_notifications")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Solo procesamos el último hijo para no saturar al inicio
                // En un caso real mas robusto, deberíamos marcar como 'leido'
                // Aquí solo reaccionamos al cambio si es reciente
                val lastNotif = snapshot.children.lastOrNull()
                if (lastNotif != null) {
                    val read = lastNotif.child("read").getValue(Boolean::class.java) ?: false
                    val title = lastNotif.child("title").getValue(String::class.java) ?: "Nueva Notificación"
                    val message = lastNotif.child("message").getValue(String::class.java) ?: ""
                    val timestamp = lastNotif.child("timestamp").getValue(Long::class.java) ?: 0L
                    
                    // Solo mostramos si es reciente (menos de 1 minuto) y no leída
                    if (!read && (System.currentTimeMillis() - timestamp) < 60000) {
                         NotificationHelper.showNotification(context, title, message)
                         // Marcar como leída para no repetir
                         lastNotif.ref.child("read").setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error si es necesario
            }
        }
        notifRef.addValueEventListener(listener)
        
        onDispose {
            notifRef.removeEventListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        loadAppointments()
    }

    // Filtrado de lista
    val filteredAppointments = remember(appointments, currentFilter) {
        if (currentFilter == "Todas") {
            appointments
        } else {
            appointments.filter { it.status == currentFilter }
        }
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
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Panel de administración",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToWelcome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar a Inicio", tint = TextSecondary)
                    }
                },
                actions = {
                    /* Tooltip removido temporalmente por conflictos de versión */
                    IconButton(
                        onClick = onLogout
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    loadAppointments() // SnapshotListener ya es realtime, pero mantenemos esto para UX
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // CABECERA CON FECHA
                item {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = "Resumen de Hoy",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }

                // MÉTRICAS
                item {
                    val pendingCount = appointments.count { it.status == "Pendiente" }
                    val acceptedCount = appointments.count { it.status == "Confirmada" }
                    val rejectedCount = appointments.count { it.status == "Cancelada" || it.status == "Rechazada" }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                title = "Total hoy",
                                value = appointments.size.toString(),
                                icon = Icons.Default.CalendarToday,
                                color = PrimaryPink,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Pendientes",
                                value = pendingCount.toString(),
                                icon = Icons.Default.PendingActions,
                                color = StatusPendingColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                title = "Aceptadas",
                                value = acceptedCount.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = StatusAcceptedColor,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Rechazadas",
                                value = rejectedCount.toString(),
                                icon = Icons.Default.Cancel,
                                color = StatusRejectedColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // FILTROS
                item {
                    Text(
                        text = "Gestión de Citas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("Todas", "Pendiente", "Confirmada", "Completada")
                        items(filters) { filter ->
                            FilterChip(
                                selected = currentFilter == filter,
                                onClick = { currentFilter = filter },
                                label = { Text(if(filter == "Pendiente") "Pendientes" else if (filter == "Confirmada") "Aceptadas" else filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPink,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // LISTA DE CITAS
                if (filteredAppointments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
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
fun AdminAppointmentCard(
    appointment: Appointment,
    onAction: (() -> Unit, String) -> Unit,
    db: FirebaseFirestore
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA DE CITA MEJORADA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // 1. HORA (Jerarquía visual alta)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = appointment.time,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 2. NOMBRE
                    Text(
                        text = appointment.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    
                    // 3. SERVICIO
                    Text(
                        text = appointment.service,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                
                // STATUS BADGE
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // BOTONES DE ACCIÓN EXPLÍCITOS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appointment.status == "Pendiente") {
                    OutlinedButton(
                        onClick = { 
                            onAction({
                                db.collection("citas").document(appointment.id).update("status", "Rechazada")
                                    .addOnSuccessListener { Toast.makeText(context, "Cita rechazada", Toast.LENGTH_SHORT).show() }
                                    .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
                            }, "¿Estás seguro de rechazar esta cita?") 
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedColor),
                        border = BorderStroke(1.dp, StatusRejectedColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rechazar")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = { 
                            onAction({
                                db.collection("citas").document(appointment.id).update("status", "Confirmada")
                                    .addOnSuccessListener { Toast.makeText(context, "Cita aceptada", Toast.LENGTH_SHORT).show() }
                                    .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
                            }, "¿Aceptar cita de ${appointment.clientName} a las ${appointment.time}?") 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAcceptedColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aceptar")
                    }
                } else if (appointment.status == "Confirmada") {
                    OutlinedButton(
                        onClick = {
                             onAction({
                                db.collection("citas").document(appointment.id).update("status", "No Asistió")
                                    .addOnSuccessListener { Toast.makeText(context, "Marcada como No Asistió", Toast.LENGTH_SHORT).show() }
                                    .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
                            }, "¿Marcar como No Asistió?")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusNoShowColor),
                        border = BorderStroke(1.dp, StatusNoShowColor)
                    ) {
                        Text("No Asistió")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                                    Toast.makeText(context, "Servicio completado", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener { e ->
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }, "¿Completar servicio?")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompletedColor)
                    ) {
                        Text("Completar")
                    }
                } else {
                    Text(
                        text = "Acciones cerradas", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // REVIEW SECTION
            if (appointment.review != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Opinión del cliente:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (index < (appointment.review["rating"] as? Long ?: 0)) Color(0xFFFFD700) else Color.Gray
                                )
                            }
                        }
                        if ((appointment.review["comment"] as? String)?.isNotEmpty() == true) {
                            Text(
                                text = appointment.review["comment"] as String,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper para oscurecer color
fun Color.darken(factor: Float): Color {
    return Color(
        red = this.red * (1 - factor),
        green = this.green * (1 - factor),
        blue = this.blue * (1 - factor),
        alpha = this.alpha
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
