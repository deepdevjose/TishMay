package com.example.esteticaapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.VibrantPink
import com.example.esteticaapp.ui.theme.GoldAccent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val appointments = remember { mutableStateListOf<Appointment>() }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PANEL ADMIN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                    loadAppointments()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                item {
                    Text(
                        text = "Resumen de Hoy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val pendingCount = appointments.count { it.status == "Pendiente" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Citas",
                            value = appointments.size.toString(),
                            icon = Icons.Default.CalendarToday,
                            color = PrimaryPink,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Pendientes",
                            value = pendingCount.toString(),
                            icon = Icons.Default.PendingActions,
                            color = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text(
                        text = "Gestión de Citas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(appointments) { appointment ->
                    AdminAppointmentCard(
                        appointment = appointment,
                        onConfirm = {
                            db.collection("citas").document(appointment.id).update("status", "Confirmada")
                        },
                        onComplete = {
                            db.runTransaction { transaction ->
                                val apptRef = db.collection("citas").document(appointment.id)
                                val userRef = db.collection("clientes").document(appointment.userId)
                                
                                transaction.update(apptRef, "status", "Completada")
                                transaction.update(userRef, "completedAppointmentsCount", FieldValue.increment(1))
                            }
                        },
                        onNoShow = {
                            db.collection("citas").document(appointment.id).update("status", "No Asistió")
                        },
                        onCancel = {
                            db.collection("citas").document(appointment.id).update("status", "Cancelada")
                        }
                    )
                }
            }
        }
    }
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

@Composable
fun AdminAppointmentCard(
    appointment: Appointment,
    onConfirm: () -> Unit,
    onComplete: () -> Unit,
    onNoShow: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${appointment.service} • ${appointment.time}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = when(appointment.status) {
                            "Confirmada" -> Color(0xFFE8F5E9)
                            "Completada" -> Color(0xFFE3F2FD)
                            "Cancelada" -> Color(0xFFFFEBEE)
                            "No Asistió" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFF5F5F5)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = appointment.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when(appointment.status) {
                                "Confirmada" -> Color(0xFF2E7D32)
                                "Completada" -> Color(0xFF2196F3)
                                "Cancelada" -> Color(0xFFC62828)
                                "No Asistió" -> Color(0xFFE65100)
                                else -> Color(0xFF616161)
                            }
                        )
                    }
                }

                Row {
                    if (appointment.status == "Pendiente") {
                        IconButton(onClick = onConfirm) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Confirmar", tint = Color(0xFF4CAF50))
                        }
                    }
                    if (appointment.status == "Confirmada") {
                        IconButton(onClick = onComplete) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Completar", tint = Color(0xFF2196F3))
                        }
                        IconButton(onClick = onNoShow) {
                            Icon(Icons.Default.PersonOff, contentDescription = "No Asistió", tint = Color(0xFFE65100))
                        }
                    }
                    if (appointment.status != "Cancelada" && appointment.status != "Completada" && appointment.status != "No Asistió") {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancelar", tint = VibrantPink)
                        }
                    }
                }
            }

            if (appointment.review != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
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
