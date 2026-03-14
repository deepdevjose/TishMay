package com.example.esteticaapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esteticaapp.HomeViewModel
import com.example.esteticaapp.ui.theme.*
import com.example.esteticaapp.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBook: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userName = uiState.userName

    Scaffold(
        containerColor = BackgroundPink
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimensions.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacerExtraLarge),
            contentPadding = PaddingValues(top = Dimensions.PaddingExtraLarge, bottom = Dimensions.PaddingExtraLarge)
        ) {
            // Header: Bienvenida Personalizada
            item {
                Column {
                    Text(
                        text = if (userName.isNotEmpty()) "Hola, $userName ✨" else "Bienvenida ✨",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Hoy es un día perfecto para brillar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            // Featured Card: IA Analysis (Glassmorphism inspired)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusHuge))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPink, DarkRose)
                            )
                        )
                        .clickable { onNavigateToScan() }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(Dimensions.PaddingLarge)
                            .align(Alignment.CenterStart)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)
                        ) {
                            Text(
                                text = "NUEVO",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(Dimensions.SpacerMedium))
                        Text(
                            text = "Análisis IA",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Descubre el diseño que mejor\nresalta tu mirada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Dimensions.PaddingLarge)
                            .size(64.dp),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            item {
                Text(
                    text = "Explorar Servicios",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            // Grid-like buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacerMedium)
                ) {
                    QuickActionCard(
                        title = "Agendar",
                        icon = Icons.Default.CalendarMonth,
                        containerColor = SoftRose,
                        contentColor = DarkRose,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBook
                    )
                    QuickActionCard(
                        title = "Mis Citas",
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        containerColor = Color.White,
                        contentColor = TextPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAppointments,
                        hasBorder = true
                    )
                }
            }

            // Beauty Tip / Promo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusExtraLarge),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(Dimensions.PaddingExtraLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimensions.IconSizeExtraLarge)
                                .background(GoldAccent.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = GoldAccent)
                        }
                        Spacer(modifier = Modifier.width(Dimensions.SpacerMedium))
                        Column {
                            Text(
                                text = "Tip del día",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Limpia tus pestañas con agua micelar libre de aceite para mayor duración.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    hasBorder: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (hasBorder) androidx.compose.foundation.BorderStroke(1.dp, DividerColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
