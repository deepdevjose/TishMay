package com.example.esteticaapp.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.NetworkUtils
import com.example.esteticaapp.NotificationHelper
import com.example.esteticaapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgendaScreen() {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val appointments = remember { mutableStateListOf<Appointment>() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    
    var showCancelDialog by remember { mutableStateOf<Appointment?>(null) }
    var showReviewDialog by remember { mutableStateOf<Appointment?>(null) }
    var cancellationCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showConfetti by remember { mutableStateOf(false) }

    fun loadData() {
        if (currentUser != null) {
            db.collection("clientes").document(currentUser.uid).get()
                .addOnSuccessListener { snapshot ->
                    cancellationCount = (snapshot?.get("cancellationCount") as? Long)?.toInt() ?: 0
                }
                .addOnFailureListener {
                    if (!NetworkUtils.isOnline(context)) {
                        errorMessage = "No hay conexión a internet. Los datos mostrados pueden no estar actualizados."
                    }
                }

            db.collection("citas")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        if (!NetworkUtils.isOnline(context)) {
                            errorMessage = "Sin conexión. No se pueden cargar las citas."
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        appointments.clear()
                        for (doc in snapshot.documents) {
                            val appt = doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                            if (appt != null) appointments.add(appt)
                        }
                        errorMessage = null
                    }
                }
        }
    }

    LaunchedEffect(currentUser) {
        loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { 
                        if (NetworkUtils.isOnline(context)) {
                            showSheet = true 
                        } else {
                            Toast.makeText(context, "Requiere conexión a internet para agendar", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = PrimaryPink,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva Cita", fontWeight = FontWeight.Bold) }
                )
            },
            containerColor = BackgroundPink
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        if (NetworkUtils.isOnline(context)) {
                            loadData()
                            errorMessage = null
                        } else {
                            errorMessage = "No hay conexión para actualizar."
                        }
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tu Agenda",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextPrimary
                            )
                            Surface(
                                color = if (cancellationCount >= 3) ErrorRed.copy(alpha = 0.1f) else GoldAccent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Cancelaciones: $cancellationCount / 3",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cancellationCount >= 3) ErrorRed else GoldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, SoftRose, CircleShape)
                                .clickable { showAvailabilityDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Ver disponibilidad", tint = PrimaryPink, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    if (appointments.isEmpty() && errorMessage == null) {
                        EmptyAgendaState()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            itemsIndexed(appointments) { index, appointment ->
                                AppointmentItemPremium(
                                    appointment = appointment,
                                    index = index,
                                    onClick = { 
                                        if (NetworkUtils.isOnline(context)) {
                                            when (appointment.status) {
                                                "Completada" -> {
                                                    if (appointment.review == null) {
                                                        showReviewDialog = appointment
                                                    }
                                                }
                                                "Cancelada", "No Asistió" -> { /* No hacer nada */ }
                                                else -> {
                                                    showCancelDialog = appointment
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Se requiere internet para esta acción", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = SoftRose) }
                ) {
                    AppointmentForm(
                        onConfirm = { newAppt ->
                            if (!NetworkUtils.isOnline(context)) {
                                Toast.makeText(context, "Sin conexión. No se pudo crear la cita.", Toast.LENGTH_LONG).show()
                                return@AppointmentForm
                            }
                            val apptWithId = newAppt.copy(
                                userId = currentUser?.uid ?: "",
                                clientName = currentUser?.displayName ?: "Usuario",
                                timestamp = System.currentTimeMillis()
                            )
                            db.collection("citas").add(apptWithId)
                                .addOnSuccessListener {
                                    showSheet = false
                                    Toast.makeText(context, "Cita agendada con éxito", Toast.LENGTH_SHORT).show()
                                    NotificationHelper.showNotification(
                                        context,
                                        "Cita Agendada",
                                        "Has agendado un servicio de ${newAppt.service} para el ${newAppt.date} a las ${newAppt.time}."
                                    )
                                }
                                .addOnFailureListener {
                                    errorMessage = "Error al guardar la cita. Intenta de nuevo."
                                }
                        },
                        onCancel = { showSheet = false }
                    )
                }
            }

            if (showAvailabilityDialog) {
                AvailabilityDialog(onDismiss = { showAvailabilityDialog = false })
            }

            if (showCancelDialog != null) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = null },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = Color.White,
                    title = { Text("¿Cancelar cita?", fontWeight = FontWeight.Bold) },
                    text = { Text("Esta acción aumentará tu contador de cancelaciones. Límite máximo: 3.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (!NetworkUtils.isOnline(context)) {
                                    Toast.makeText(context, "Sin conexión para cancelar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (cancellationCount < 3) {
                                    val appt = showCancelDialog!!
                                    db.runTransaction { transaction ->
                                        val userRef = db.collection("clientes").document(currentUser!!.uid)
                                        val apptRef = db.collection("citas").document(appt.id)
                                        
                                        transaction.update(userRef, "cancellationCount", FieldValue.increment(1))
                                        transaction.update(apptRef, "status", "Cancelada")
                                    }.addOnSuccessListener {
                                        showCancelDialog = null
                                        errorMessage = null
                                    }.addOnFailureListener {
                                        errorMessage = "Error al procesar la cancelación"
                                    }
                                } else {
                                    errorMessage = "Has alcanzado el límite de cancelaciones."
                                    showCancelDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Confirmar Cancelación")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = null }) {
                            Text("Regresar", color = TextSecondary)
                        }
                    }
                )
            }

            if (showReviewDialog != null) {
                ReviewDialog(
                    appointment = showReviewDialog!!,
                    onDismiss = { showReviewDialog = null },
                    onReviewSubmit = { rating, comment ->
                        if (!NetworkUtils.isOnline(context)) {
                            Toast.makeText(context, "Sin conexión para enviar reseña", Toast.LENGTH_SHORT).show()
                            return@ReviewDialog
                        }
                        val review = mapOf("rating" to rating, "comment" to comment)
                        db.collection("citas").document(showReviewDialog!!.id)
                            .update("review", review)
                            .addOnSuccessListener {
                                showReviewDialog = null
                                showConfetti = true
                                scope.launch {
                                    delay(4000)
                                    showConfetti = false
                                }
                            }
                    }
                )
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 30f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                        position = Position.Relative(0.5, 0.3),
                        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                    )
                )
            )
        }
    }
}

@Composable
fun AvailabilityDialog(onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())) }
    var occupiedSlots by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    val timeSlots = listOf(
        "10:00 AM", "11:00 AM", "12:00 PM", "01:00 PM", 
        "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
    )

    fun checkAvailability(date: String) {
        db.collection("citas")
            .whereEqualTo("date", date)
            .whereIn("status", listOf("Pendiente", "Confirmada"))
            .get()
            .addOnSuccessListener { snapshot ->
                val counts = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val time = doc.getString("time") ?: ""
                    counts[time] = (counts[time] ?: 0) + 1
                }
                occupiedSlots = counts
            }
    }

    LaunchedEffect(selectedDate) {
        checkAvailability(selectedDate)
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCalendar = Calendar.getInstance()
            pickedCalendar.set(year, month, dayOfMonth)
            selectedDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(pickedCalendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Disponibilidad", fontWeight = FontWeight.Black) },
        text = {
            Column {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = PrimaryPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedDate, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(timeSlots) { time ->
                        val count = occupiedSlots[time] ?: 0
                        val isFull = count >= 2
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFull) Color.LightGray.copy(alpha = 0.3f) else SoftRose.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(time, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isFull) "Ocupado" else "Disponible (${2 - count})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isFull) Color.Gray else PrimaryPink
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EmptyAgendaState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(SoftRose.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EventNote,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = PrimaryPink
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sin citas próximas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¡Es el momento perfecto para consentirte!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentForm(
    onConfirm: (Appointment) -> Unit,
    onCancel: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var selectedService by remember { mutableStateOf("Microblading") }
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(sdf.format(calendar.time)) }
    var selectedTime by remember { mutableStateOf("") }
    var occupiedSlots by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    val context = LocalContext.current
    val services = listOf("Microblading", "Lash Lifting", "Diseño de Cejas", "Extensión de Pestañas")
    val timeSlots = listOf("10:00 AM", "11:00 AM", "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM")

    fun checkAvailability(date: String) {
        db.collection("citas")
            .whereEqualTo("date", date)
            .whereIn("status", listOf("Pendiente", "Confirmada"))
            .get()
            .addOnSuccessListener { snapshot ->
                val counts = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val time = doc.getString("time") ?: ""
                    counts[time] = (counts[time] ?: 0) + 1
                }
                occupiedSlots = counts
            }
    }

    LaunchedEffect(selectedDate) {
        checkAvailability(selectedDate)
        selectedTime = "" // Reset time when date changes
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCalendar = Calendar.getInstance()
            pickedCalendar.set(year, month, dayOfMonth)
            // Verificar que no sea domingo (Calendar.SUNDAY = 1)
            if (pickedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                Toast.makeText(context, "No abrimos los domingos", Toast.LENGTH_SHORT).show()
            } else {
                selectedDate = sdf.format(pickedCalendar.time)
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)
    ) {
        Text("Nueva Cita", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Servicio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            services.forEach { service ->
                FilterChip(
                    selected = selectedService == service,
                    onClick = { selectedService = service },
                    label = { Text(service) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPink,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Fecha", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SoftRose)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = PrimaryPink)
                Spacer(modifier = Modifier.width(16.dp))
                Text(selectedDate, modifier = Modifier.weight(1f))
                Text("Cambiar", color = PrimaryPink, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Horario (Máx 2 por hora)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(150.dp)
        ) {
            items(timeSlots) { time ->
                val count = occupiedSlots[time] ?: 0
                val isFull = count >= 2
                FilterChip(
                    selected = selectedTime == time,
                    onClick = { if (!isFull) selectedTime = time },
                    enabled = !isFull,
                    label = { Text(time, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPink,
                        selectedLabelColor = Color.White,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                if (selectedTime.isNotEmpty()) {
                    onConfirm(Appointment(service = selectedService, date = selectedDate, time = selectedTime)) 
                } else {
                    Toast.makeText(context, "Por favor selecciona un horario", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
        ) {
            Text("Agendar Ahora", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReviewDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onReviewSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("¿Qué te pareció?", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tu opinión nos ayuda a mejorar", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.padding(vertical = 20.dp)) {
                    repeat(5) { i ->
                        Icon(
                            if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                            null,
                            modifier = Modifier.size(40.dp).clickable { rating = i + 1 },
                            tint = if (i < rating) Color(0xFFFFD700) else Color.LightGray
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Escribe un comentario...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPink)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onReviewSubmit(rating, comment) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)) {
                Text("Enviar")
            }
        }
    )
}

@Composable
fun AppointmentItemPremium(
    appointment: Appointment,
    index: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer {
                translationY = 0f
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(PrimaryPink.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (appointment.service) {
                        "Microblading" -> Icons.Default.Face
                        "Lash Lifting" -> Icons.Default.Visibility
                        else -> Icons.Default.AutoAwesome
                    },
                    contentDescription = null,
                    tint = PrimaryPink
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.service,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${appointment.date} • ${appointment.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            StatusBadge(status = appointment.status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Pendiente" -> GoldAccent
        "Confirmada" -> PrimaryPink
        "Completada" -> Color(0xFF4CAF50)
        "Cancelada" -> ErrorRed
        else -> Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
