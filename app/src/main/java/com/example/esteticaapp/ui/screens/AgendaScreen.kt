package com.example.esteticaapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.NetworkUtils
import com.example.esteticaapp.R
import com.example.esteticaapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
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
    var isReadOnlyMode by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    
    var showCancelDialog by remember { mutableStateOf<Appointment?>(null) }
    var showReviewDialog by remember { mutableStateOf<Appointment?>(null) }
    var showDetailSheet by remember { mutableStateOf<Appointment?>(null) }
    var cancellationCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var currentUserName by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var showConfetti by remember { mutableStateOf(false) }

    val today = remember { Date() }
    val sdf = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val monthSdf = remember { SimpleDateFormat("MMM, yyyy", Locale.getDefault()) }
    
    val activeAppointments = remember(appointments) { 
        appointments.filter { 
            it.status == "Pendiente" || it.status == "Confirmada" || it.status == "Completada" 
        } 
    }
    
    val todayStr = remember { sdf.format(today) }
    val currentMonthStr = remember { monthSdf.format(today) }
    
    val dailyCount = remember(activeAppointments) { activeAppointments.count { it.date == todayStr } }
    val monthlyCount = remember(activeAppointments) { activeAppointments.count { it.date.contains(currentMonthStr) } }

    DisposableEffect(currentUser) {
        if (currentUser == null) return@DisposableEffect onDispose {}

        val userListener = db.collection("clientes").document(currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    cancellationCount = (snapshot.get("cancellationCount") as? Long)?.toInt() ?: 0
                    val fName = snapshot.getString("firstName") ?: ""
                    val lName = snapshot.getString("lastName") ?: ""
                    currentUserName = if (fName.isNotEmpty()) "$fName $lName" else (currentUser.displayName ?: "Usuario")
                }
            }

        val appointmentsListener = db.collection("citas")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = "Error al sincronizar: ${e.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    appointments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                    }
                    errorMessage = null
                }
            }

        onDispose {
            userListener.remove()
            appointmentsListener.remove()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { 
                        if (NetworkUtils.isOnline(context)) {
                            isReadOnlyMode = false
                            showSheet = true 
                        } else {
                            Toast.makeText(context, "Requiere conexión a internet", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = PrimaryPink,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Reservar Cita", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            },
            containerColor = BackgroundPink
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        delay(800)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 32.dp, bottom = 100.dp)
                ) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tu Agenda",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                                    )
                                    Text(
                                        text = "Administra tus citas y disponibilidad",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        isReadOnlyMode = true
                                        showSheet = true 
                                    },
                                    modifier = Modifier
                                        .background(Color.White, CircleShape)
                                        .border(1.dp, SoftRose, CircleShape)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Ver disponibilidad", tint = PrimaryPink)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SummaryChip(
                                    label = "Hoy",
                                    current = dailyCount,
                                    max = 2,
                                    icon = Icons.Default.Today
                                )

                                SummaryChip(
                                    label = "Mes",
                                    current = monthlyCount,
                                    max = 5,
                                    icon = Icons.Default.DateRange
                                )

                                SummaryChip(
                                    label = "Cancelaciones",
                                    current = cancellationCount,
                                    max = 3,
                                    icon = Icons.Default.History
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                            ) {
                                Text(errorMessage!!, modifier = Modifier.padding(12.dp), color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (appointments.isEmpty() && !isRefreshing) {
                        item {
                            EmptyAgendaState(onViewAvailability = {
                                isReadOnlyMode = true
                                showSheet = true
                            })
                        }
                    } else {
                        itemsIndexed(appointments, key = { _, item -> item.id }) { _, appointment ->
                            AppointmentItemPremium(
                                appointment = appointment,
                                onClick = { 
                                    showDetailSheet = appointment
                                }
                            )
                        }
                    }
                }
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    AppointmentForm(
                        userId = currentUser?.uid ?: "",
                        cancellationCount = cancellationCount,
                        isReadOnly = isReadOnlyMode,
                        onConfirm = { newAppt ->
                            val finalName = currentUserName.ifEmpty { (currentUser?.displayName ?: "Usuario") }
                            val apptWithId = newAppt.copy(
                                userId = currentUser?.uid ?: "",
                                clientName = finalName,
                                timestamp = System.currentTimeMillis()
                            )
                            db.collection("citas").add(apptWithId).addOnSuccessListener {
                                val rtdb = FirebaseDatabase.getInstance("https://estetica-e0333-default-rtdb.firebaseio.com")
                                rtdb.getReference("admin_notifications").push().setValue(mapOf(
                                    "title" to "Nueva Cita",
                                    "message" to "$finalName agendó ${newAppt.service}",
                                    "timestamp" to System.currentTimeMillis(),
                                    "read" to false
                                ))
                                showSheet = false
                                Toast.makeText(context, "Cita agendada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancel = { showSheet = false }
                    )
                }
            }

            if (showDetailSheet != null) {
                ModalBottomSheet(
                    onDismissRequest = { showDetailSheet = null },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    AppointmentDetailSheet(
                        appointment = showDetailSheet!!,
                        onCancel = {
                            if (NetworkUtils.isOnline(context)) {
                                showDetailSheet = null
                                showCancelDialog = it
                            } else {
                                Toast.makeText(context, "Requiere conexión a internet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReview = {
                            if (NetworkUtils.isOnline(context)) {
                                showDetailSheet = null
                                showReviewDialog = it
                            }
                        },
                        onClose = { showDetailSheet = null }
                    )
                }
            }

            if (showCancelDialog != null) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = null },
                    shape = RoundedCornerShape(20.dp),
                    title = { Text("¿Cancelar cita?", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Esta acción no se puede deshacer.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Esta cancelación se registrará en tu cuenta.")
                            val remaining = 3 - cancellationCount
                            Text(
                                text = "Te quedarán $remaining de 3 cancelaciones este mes.",
                                fontWeight = FontWeight.Bold,
                                color = if (remaining <= 1) ErrorRed else Color.Unspecified
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (cancellationCount < 3) {
                                    val appt = showCancelDialog!!
                                    db.runTransaction { transaction ->
                                        transaction.update(db.collection("clientes").document(currentUser!!.uid), "cancellationCount", FieldValue.increment(1))
                                        transaction.update(db.collection("citas").document(appt.id), "status", "Cancelada")
                                    }.addOnSuccessListener { showCancelDialog = null }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Confirmar") }
                    },
                    dismissButton = { 
                        TextButton(onClick = { showCancelDialog = null }) { 
                            Text("Volver", color = Color.Gray) 
                        } 
                    }
                )
            }

            if (showReviewDialog != null) {
                ReviewDialog(
                    onDismiss = { showReviewDialog = null },
                    onReviewSubmit = { rating, comment ->
                        db.collection("citas").document(showReviewDialog!!.id).update("review", mapOf("rating" to rating, "comment" to comment))
                            .addOnSuccessListener {
                                showReviewDialog = null
                                showConfetti = true
                                Toast.makeText(context, "¡Gracias por tu opinión!", Toast.LENGTH_SHORT).show()
                                scope.launch { delay(3000); showConfetti = false }
                            }
                    }
                )
            }
        }

        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(Party(speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360, colors = listOf(0xfce18a, 0xff726d, 0xf4306d), position = Position.Relative(0.5, 0.3), emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)))
            )
        }
    }
}

@Composable
fun borderStrokePink() = BorderStroke(1.dp, SoftRose)

@Composable
fun SummaryChip(label: String, current: Int, max: Int, icon: ImageVector) {
    val isLimitReached = current >= max
    val color = if (isLimitReached) ErrorRed else PrimaryPink
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(6.dp))
            Text("$label: $current/$max", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyAgendaState(onViewAvailability: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(100.dp).background(SoftRose.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.EventNote, null, modifier = Modifier.size(50.dp), tint = PrimaryPink)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Sin citas próximas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Revisa horarios disponibles o agenda tu próximo servicio.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onViewAvailability,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SoftRose)
        ) {
            Text("Ver horarios", color = PrimaryPink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppointmentForm(
    userId: String,
    cancellationCount: Int,
    isReadOnly: Boolean = false,
    onConfirm: (Appointment) -> Unit,
    onCancel: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var selectedService by remember { mutableStateOf("Microblading") }
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(sdf.format(Date())) }
    var selectedTime by remember { mutableStateOf("") }
    
    var occupiedSlots by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var userHourCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var userDailyCount by remember { mutableIntStateOf(0) }
    var userMonthlyCount by remember { mutableIntStateOf(0) }
    var maxCapacity by remember { mutableIntStateOf(2) }

    val context = LocalContext.current
    val services = listOf("Microblading", "Lash Lifting", "Diseño de Cejas", "Extensión de Pestañas")
    val serviceIcons = mapOf(
        "Microblading" to Icons.Default.Face,
        "Lash Lifting" to Icons.Default.AutoAwesome,
        "Diseño de Cejas" to Icons.Default.ContentCut,
        "Extensión de Pestañas" to Icons.Default.Visibility
    )
    val timeSlots = listOf("10:00 AM", "12:00 PM", "02:00 PM", "04:00 PM")

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = utcTimeMillis
                val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                val isPast = utcTimeMillis < (System.currentTimeMillis() - 86400000)
                return !isSunday && !isPast
            }
        }
    )

    LaunchedEffect(Unit) {
        db.collection("config").document("appointments").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                maxCapacity = (snapshot.get("maxCapacityPerHour") as? Number)?.toInt() ?: 2
            }
        }
    }

    DisposableEffect(selectedDate) {
        val monthSdf = SimpleDateFormat("MMM, yyyy", Locale.getDefault())
        val currentMonthStr = monthSdf.format(sdf.parse(selectedDate) ?: Date())

        val registration = db.collection("citas")
            .whereIn("status", listOf("Pendiente", "Confirmada", "Completada"))
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val counts = mutableMapOf<String, Int>()
                    val uCounts = mutableMapOf<String, Int>()
                    var daily = 0
                    var monthly = 0
                    
                    for (doc in snapshot.documents) {
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val uid = doc.getString("userId") ?: ""
                        
                        if (date == selectedDate) {
                            counts[time] = (counts[time] ?: 0) + 1
                            if (uid == userId) {
                                uCounts[time] = (uCounts[time] ?: 0) + 1
                                daily++
                            }
                        }
                        
                        if (uid == userId && date.contains(currentMonthStr)) {
                            monthly++
                        }
                    }
                    occupiedSlots = counts
                    userHourCounts = uCounts
                    userDailyCount = daily
                    userMonthlyCount = monthly
                }
            }
        onDispose { registration.remove() }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        // El DatePickerState devuelve la fecha en UTC (medianoche).
                        // Para evitar el desfase por zona horaria al formatear, usamos UTC explícitamente.
                        val utcFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        selectedDate = utcFormat.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("Seleccionar", color = PrimaryPink, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { 
                    Text("Cancelar", color = Color.Gray) 
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (isReadOnly) "Disponibilidad" else "Nueva Cita", 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Selecciona servicio, fecha y horario disponible",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryChip(label = "Hoy", current = userDailyCount, max = 2, icon = Icons.Default.Today)
            SummaryChip(label = "Mes", current = userMonthlyCount, max = 5, icon = Icons.Default.DateRange)
            SummaryChip(label = "Cancelaciones", current = cancellationCount, max = 3, icon = Icons.Default.History)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isReadOnly) {
            Text("Servicio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                services.forEach { s ->
                    ServiceChip(
                        name = s,
                        icon = serviceIcons[s] ?: Icons.Default.AutoAwesome,
                        isSelected = selectedService == s,
                        onClick = { selectedService = s }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("Fecha", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(12.dp),
            border = borderStrokePink(),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = PrimaryPink, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(selectedDate, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = "No se muestran horarios pasados del día actual",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Horarios disponibles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Capacidad: $maxCapacity pers.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val rows = timeSlots.chunked(2)
        rows.forEach { rowSlots ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEach { t ->
                    TimeSlotCard(
                        time = t,
                        selectedTime = selectedTime,
                        maxCapacity = maxCapacity,
                        occupiedCount = occupiedSlots[t] ?: 0,
                        isPastTime = isPastTime(t, selectedDate, sdf),
                        alreadyBookedByMe = (userHourCounts[t] ?: 0) >= 1,
                        dailyLimitReached = userDailyCount >= 2,
                        isReadOnly = isReadOnly,
                        onSelect = { selectedTime = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            InfoNotice(text = "Puedes coincidir con una amiga o amigo en el mismo horario", icon = Icons.Default.People, color = PrimaryPink)
            InfoNotice(text = "Si desean venir juntos, cada uno debe reservar desde su cuenta")
            InfoNotice(text = "Cada horario admite hasta $maxCapacity personas")
            InfoNotice(text = "Solo puedes reservar 2 citas por día")
            if (userDailyCount >= 2) {
                InfoNotice(text = "Para una tercera cita, contáctanos por WhatsApp", color = Color(0xFF128C7E))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        val dailyLimitReached = userDailyCount >= 2
        val monthlyLimitReached = userMonthlyCount >= 5

        if (!isReadOnly) {
            if (monthlyLimitReached) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
                    ) { Text("Has alcanzado tu límite de 5 citas este mes", color = Color.Gray) }
                }
            } else if (dailyLimitReached) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/525560115704"))) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(painterResource(R.drawable.ic_whatsapp), null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reservar por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Límite de 2 citas para hoy alcanzado. Contáctanos por WhatsApp.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF128C7E), textAlign = TextAlign.Center)
                }
            } else {
                Button(
                    onClick = { if (selectedTime.isNotEmpty()) onConfirm(Appointment(service = selectedService, date = selectedDate, time = selectedTime)) }, 
                    modifier = Modifier.fillMaxWidth().height(52.dp), 
                    enabled = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = if (selectedTime.isEmpty()) {
                        ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f), contentColor = Color.Gray)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = PrimaryPink, contentColor = Color.White)
                    }
                ) { 
                    Text(
                        if (selectedTime.isEmpty()) "Selecciona horario para continuar" else "Agendar Cita",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            }
        }
        
        TextButton(
            onClick = onCancel, 
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text(if (isReadOnly) "Cerrar" else "Cancelar", color = Color.Gray, fontWeight = FontWeight.Medium) 
        }
    }
}

private fun isPastTime(time: String, selectedDate: String, sdf: SimpleDateFormat): Boolean {
    val calendarNow = Calendar.getInstance()
    if (selectedDate != sdf.format(calendarNow.time)) return false
    
    return try {
        val slotTime = SimpleDateFormat("hh:mm a", Locale.US).parse(time)
        val slotCal = Calendar.getInstance().apply {
            this.time = slotTime!!
            set(Calendar.YEAR, calendarNow.get(Calendar.YEAR))
            set(Calendar.MONTH, calendarNow.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calendarNow.get(Calendar.DAY_OF_MONTH))
        }
        slotCal.timeInMillis < (System.currentTimeMillis() - 900000)
    } catch (e: Exception) { false }
}

@Composable
fun ServiceChip(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) PrimaryPink else Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) PrimaryPink else SoftRose),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) Color.White else PrimaryPink)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name, 
                color = if (isSelected) Color.White else TextPrimary, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun RowScope.TimeSlotCard(
    time: String,
    selectedTime: String,
    maxCapacity: Int,
    occupiedCount: Int,
    isPastTime: Boolean,
    alreadyBookedByMe: Boolean,
    dailyLimitReached: Boolean,
    isReadOnly: Boolean,
    onSelect: (String) -> Unit
) {
    val isFull = occupiedCount >= maxCapacity
    val blocked = isFull || alreadyBookedByMe || (dailyLimitReached && !isReadOnly) || isPastTime
    
    val statusText = when {
        isPastTime -> "Pasado"
        isFull -> "Lleno"
        alreadyBookedByMe -> "Tu cita"
        else -> {
            val rem = maxCapacity - occupiedCount
            if (rem == 1) "1 lugar disponible" else "$rem lugares"
        }
    }

    Card(
        modifier = Modifier
            .weight(1f)
            .alpha(if (blocked && !isReadOnly) 0.5f else 1f)
            .clickable(enabled = !blocked && !isReadOnly) { onSelect(time) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedTime == time) PrimaryPink.copy(alpha = 0.08f) else Color.White
        ),
        border = BorderStroke(
            width = if (selectedTime == time) 2.dp else 1.dp,
            color = if (selectedTime == time) PrimaryPink else SoftRose
        ),
        elevation = CardDefaults.cardElevation(if (selectedTime == time) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selectedTime == time) PrimaryPink else TextPrimary)
            Text(statusText, fontSize = 11.sp, color = if (isFull) ErrorRed else if (selectedTime == time) PrimaryPink else Color.Gray)
        }
    }
}

@Composable
fun InfoNotice(text: String, icon: ImageVector = Icons.Default.Info, color: Color = Color.Gray) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = color.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReviewDialog(onDismiss: () -> Unit, onReviewSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, 
        shape = RoundedCornerShape(24.dp),
        title = { Text("¿Cómo fue tu experiencia?", fontWeight = FontWeight.Bold) }, 
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row { repeat(5) { i -> Icon(if(i < rating) Icons.Default.Star else Icons.Default.StarBorder, null, modifier = Modifier.size(36.dp).clickable { rating = i + 1 }, tint = if(i < rating) Color(0xFFFFD700) else Color.LightGray) } }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment, 
                    onValueChange = { comment = it }, 
                    placeholder = { Text("Tu comentario es opcional", color = Color.LightGray) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPink, unfocusedBorderColor = SoftRose)
                )
            }
        }, 
        confirmButton = { 
            Button(
                onClick = { onReviewSubmit(rating, comment) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) { Text("Enviar", fontWeight = FontWeight.Bold) } 
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Más tarde", color = Color.Gray) }
        }
    )
}

@Composable
fun AppointmentItemPremium(appointment: Appointment, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), 
        shape = RoundedCornerShape(20.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White), 
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).background(PrimaryPink.copy(alpha = 0.1f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(if(appointment.service == "Microblading") Icons.Default.Face else Icons.Default.AutoAwesome, null, tint = PrimaryPink, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appointment.service, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${appointment.date} • ${appointment.time}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                val (microcopy, statusColor) = when(appointment.status) {
                    "Pendiente" -> "En revisión" to GoldAccent
                    "Confirmada" -> "Tu cita está lista" to PrimaryPink
                    "Completada" -> (if (appointment.review == null) "Ya puedes calificar" else "¡Gracias por tu visita!") to Color(0xFF4CAF50)
                    else -> "" to Color.Gray
                }
                
                if (microcopy.isNotEmpty()) {
                    Text(
                        text = microcopy,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            StatusBadge(status = appointment.status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when(status) { "Pendiente" -> GoldAccent; "Confirmada" -> PrimaryPink; "Completada" -> Color(0xFF4CAF50); "Cancelada", "Rechazada" -> ErrorRed; else -> Color.Gray }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppointmentDetailSheet(
    appointment: Appointment,
    onCancel: (Appointment) -> Unit,
    onReview: (Appointment) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(PrimaryPink.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (appointment.service == "Microblading") Icons.Default.Face else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = appointment.service,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        
        Text(
            text = "${appointment.date} • ${appointment.time}",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        StatusBadge(status = appointment.status)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Acciones disponibles",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        when (appointment.status) {
            "Pendiente", "Confirmada" -> {
                Button(
                    onClick = { onCancel(appointment) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f), contentColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelar Cita", fontWeight = FontWeight.Bold)
                }
            }
            "Completada" -> {
                if (appointment.review == null) {
                    Button(
                        onClick = { onReview(appointment) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calificar Experiencia", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("¡Gracias por tu calificación!", color = Color.Gray)
                    }
                }
            }
            else -> {
                Text(
                    text = "No hay acciones para este estado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}
