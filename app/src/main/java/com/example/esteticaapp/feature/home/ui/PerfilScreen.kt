/**
 * Pantalla de Perfil de usuario para la aplicación TishMay.
 * Gestiona la información personal, el historial de análisis de mirada IA,
 * y el sistema de logros y recompensas (fidelidad, embajadora, etc.).
 * 
 * Tecnologías y conceptos clave:
 * - Firebase Firestore: Escucha en tiempo real de los datos del perfil y logros.
 * - Coil: Carga dinámica de avatares e imágenes de perfil.
 * - Sistema de Gamificación: Cálculo dinámico de niveles (Básico, VIP, Elite) y
 *   visualización de progreso mediante barras de estado.
 * - Diálogos Personalizados: Visualización detallada de diagnósticos previos de IA.
 */

package com.example.esteticaapp.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.esteticaapp.R
import com.example.esteticaapp.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class Achievement(
    val id: String,
    val title: String,
    val subtitle: String,
    val reward: String,
    val icon: ImageVector,
    val currentProgress: Int,
    val targetProgress: Int,
    val isUnlocked: Boolean,
    val category: String,
    val isStreak: Boolean = false
)

@Composable
fun PerfilScreen(onLogout: () -> Unit, onNavigateToAgenda: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    val avatarIcons = mapOf(
        "face_3" to Icons.Default.Face3,
        "face_4" to Icons.Default.Face4,
        "woman" to Icons.Default.Woman,
        "person" to Icons.Default.Person,
        "face" to Icons.Default.Face,
        "man" to Icons.Default.Man,
        "spa" to Icons.Default.Spa,
        "self_improvement" to Icons.Default.SelfImprovement,
        "emoji_people" to Icons.Default.EmojiPeople
    )

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("clientes").document(currentUser.uid).addSnapshotListener { snapshot, _ ->
                userData = snapshot?.data
                isLoading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundPink
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
        } else {
            val firstName = userData?.get("firstName") as? String ?: stringResource(R.string.default_username)
            val lastName = userData?.get("lastName") as? String ?: ""
            val email = userData?.get("email") as? String ?: currentUser?.email ?: ""
            val avatarKey = userData?.get("avatar") as? String
            val fullName = "$firstName $lastName".trim()
            val initials = if (firstName.isNotEmpty()) firstName.take(1).uppercase() + (if (lastName.isNotEmpty()) lastName.take(1).uppercase() else "") else "U"
            
            val completedCount = (userData?.get("completedAppointmentsCount") as? Long)?.toInt() ?: 0
            val referralsCount = (userData?.get("referralsCount") as? Long)?.toInt() ?: 0
            val punctualityStreak = (userData?.get("punctualityStreak") as? Long)?.toInt() ?: 0
            val hasExploredGallery = userData?.get("hasExploredGallery") as? Boolean ?: false
            
            @Suppress("UNCHECKED_CAST")
            val lastAnalysis = userData?.get("last_analysis") as? Map<String, Any>
            val hasUsedIA = lastAnalysis != null

            val achievementsList = listOf(
                Achievement("frequent", stringResource(R.string.achievement_frequent_title), stringResource(R.string.achievement_frequent_subtitle), stringResource(R.string.achievement_frequent_reward), Icons.Default.Star, completedCount, 3, completedCount >= 3, stringResource(R.string.category_loyalty)),
                Achievement("vip", stringResource(R.string.achievement_vip_title), stringResource(R.string.achievement_vip_subtitle), stringResource(R.string.achievement_vip_reward), Icons.Default.AutoAwesome, completedCount, 6, completedCount >= 6, stringResource(R.string.category_loyalty)),
                Achievement("ambassador", stringResource(R.string.achievement_ambassador_title), stringResource(R.string.achievement_ambassador_subtitle), stringResource(R.string.achievement_ambassador_reward), Icons.Default.Groups, referralsCount, 1, referralsCount >= 1, stringResource(R.string.category_referrals)),
                Achievement("punctual", stringResource(R.string.achievement_punctual_title), stringResource(R.string.achievement_punctual_subtitle), stringResource(R.string.achievement_punctual_reward), Icons.Default.LocalFireDepartment, punctualityStreak, 3, punctualityStreak >= 3, stringResource(R.string.category_trust), isStreak = true),
                Achievement("explorer", stringResource(R.string.achievement_explorer_title), stringResource(R.string.achievement_explorer_subtitle), stringResource(R.string.achievement_explorer_reward), Icons.Default.ImageSearch, if(hasExploredGallery) 1 else 0, 1, hasExploredGallery, stringResource(R.string.category_app)),
                Achievement("ia_pro", stringResource(R.string.achievement_ia_pro_title), stringResource(R.string.achievement_ia_pro_subtitle), stringResource(R.string.achievement_ia_pro_reward), Icons.Default.AutoFixHigh, if(hasUsedIA) 1 else 0, 1, hasUsedIA, stringResource(R.string.category_app))
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(SoftRose.copy(alpha = 0.5f), BackgroundPink)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(
                                        Brush.linearGradient(listOf(PrimaryPink.copy(alpha = 0.2f), DarkRose.copy(alpha = 0.2f))),
                                        CircleShape
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(3.dp)
                                ) {
                                    val icon = avatarIcons[avatarKey]
                                    if (icon != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(PrimaryPink.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = stringResource(R.string.content_description_avatar),
                                                modifier = Modifier.size(60.dp),
                                                tint = PrimaryPink
                                            )
                                        }
                                    } else if (!avatarKey.isNullOrEmpty() && avatarKey.startsWith("http")) {
                                        AsyncImage(
                                            model = avatarKey,
                                            contentDescription = stringResource(R.string.content_description_avatar),
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(PrimaryPink, DarkRose)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (completedCount >= 10) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                item {
                    val currentLevel = when {
                        completedCount >= 20 -> stringResource(R.string.level_elite)
                        completedCount >= 10 -> stringResource(R.string.level_vip)
                        else -> stringResource(R.string.level_basic)
                    }
                    
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatBox(
                                label = stringResource(R.string.label_appointments),
                                value = completedCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = stringResource(R.string.label_level),
                                value = currentLevel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val (nextLevel, goal) = when {
                            completedCount < 10 -> stringResource(R.string.level_vip) to 10
                            completedCount < 20 -> stringResource(R.string.level_elite) to 20
                            else -> stringResource(R.string.level_max) to 20
                        }
                        
                        if (completedCount < 20) {
                            val progress = completedCount.toFloat() / goal
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(stringResource(R.string.level_format, currentLevel), style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.level_progress_format, completedCount, goal, nextLevel), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = PrimaryPink,
                                    trackColor = Color.White
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.section_eye_analysis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (lastAnalysis != null) {
                        IADiagnosticCard(
                            data = lastAnalysis, 
                            onClick = { showDiagnosticDialog = true },
                            onBookClick = onNavigateToAgenda
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryPink.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.no_eye_analysis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.section_rewards),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(achievementsList) { achievement ->
                    AchievementCard(achievement)
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoftRose),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.btn_logout),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ErrorRed
                        )
                    }
                }
            }
        }

        if (showDiagnosticDialog && userData?.get("last_analysis") != null) {
            @Suppress("UNCHECKED_CAST")
            FullDiagnosticDialog(userData?.get("last_analysis") as Map<String, Any>) {
                showDiagnosticDialog = false
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(if (achievement.isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        if (achievement.isUnlocked) PrimaryPink.copy(alpha = 0.1f) 
                        else Color.LightGray.copy(alpha = 0.1f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    achievement.icon, 
                    contentDescription = null, 
                    tint = if (achievement.isUnlocked) PrimaryPink else Color.LightGray,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        achievement.title, 
                        style = MaterialTheme.typography.titleSmall, 
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) TextPrimary else TextSecondary
                    )
                    if (achievement.isStreak && achievement.currentProgress > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🔥 ${achievement.currentProgress}", color = Color(0xFFFF5722), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
                Text(achievement.subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!achievement.isUnlocked) {
                    LinearProgressIndicator(
                        progress = { achievement.currentProgress.toFloat() / achievement.targetProgress },
                        modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).clip(CircleShape),
                        color = PrimaryPink,
                        trackColor = BackgroundPink
                    )
                } else {
                    Text(stringResource(R.string.achievement_completed), style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (achievement.isUnlocked) SuccessGreen.copy(alpha = 0.1f) else BackgroundPink,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        achievement.reward,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (achievement.isUnlocked) SuccessGreen else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!achievement.isUnlocked) {
                    Text(
                        "${achievement.currentProgress}/${achievement.targetProgress}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IADiagnosticCard(data: Map<String, Any>, onClick: () -> Unit, onBookClick: () -> Unit) {
    val style = data["recommended_style"] as? String ?: stringResource(R.string.not_available)
    val shape = data["eye_shape"] as? String ?: stringResource(R.string.not_available)
    val timestamp = data["date"] as? Timestamp
    
    val dateStr = if (timestamp != null) {
        val sdf = SimpleDateFormat("d MMMM", Locale.forLanguageTag("es-ES"))
        stringResource(R.string.last_analysis_format, sdf.format(timestamp.toDate()))
    } else stringResource(R.string.recent)

    val styleIcon = when {
        style.contains("Ardilla", ignoreCase = true) -> "🐿️"
        style.contains("Muñeca", ignoreCase = true) -> "🧸"
        style.contains("Gato", ignoreCase = true) -> "🐱"
        style.contains("Zorro", ignoreCase = true) -> "🦊"
        style.contains("Natural", ignoreCase = true) -> "✨"
        else -> "👁️"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryPink.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryPink, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.ia_diagnostic), style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.Bold)
                }
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column {
                Text(stringResource(R.string.recommended_style_for_you), style = MaterialTheme.typography.labelSmall, color = PrimaryPink, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(style, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(styleIcon, fontSize = 28.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column {
                Text(stringResource(R.string.eye_shape), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(shape, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onBookClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_book_appointment), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.view_ia_analysis), style = MaterialTheme.typography.bodySmall, color = PrimaryPink, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FullDiagnosticDialog(data: Map<String, Any>, onDismiss: () -> Unit) {
    val style = data["recommended_style"] as? String ?: ""
    val explanation = data["explanation"] as? String ?: stringResource(R.string.no_explanation_available)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.diagnostic_detail), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BackgroundPink)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(VibrantPink, Color(0xFFFF80AB))),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.recommended_style_label), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                            Text(style, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                repeat(5) { Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F8F8), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.why_it_suits_you), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(explanation, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, lineHeight = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.diagnostic_instruction), style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPink),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = PrimaryPink)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
