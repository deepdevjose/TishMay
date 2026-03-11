package com.example.esteticaapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esteticaapp.ui.theme.DeepBlack
import com.example.esteticaapp.ui.theme.Gold
import com.example.esteticaapp.ui.theme.PalePink
import com.example.esteticaapp.ui.theme.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBook: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "LASH & BROW STUDIO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = DeepBlack
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SoftWhite
                )
            )
        },
        containerColor = SoftWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Sección de Bienvenida / Card Destacada
            item {
                WelcomeCard()
            }

            // Botones de Funciones Principales
            item {
                Text(
                    text = "Servicios y Gestión",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlack
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                MenuButton(
                    title = "Agendar Cita",
                    subtitle = "Reserva tu espacio ahora",
                    icon = Icons.Default.CalendarMonth,
                    backgroundColor = PalePink,
                    contentColor = DeepBlack,
                    onClick = onNavigateToBook
                )
            }

            item {
                MenuButton(
                    title = "Escanear Mirada con IA",
                    subtitle = "Análisis personalizado",
                    icon = Icons.Default.AutoAwesome,
                    backgroundColor = DeepBlack,
                    contentColor = PalePink,
                    onClick = onNavigateToScan
                )
            }

            item {
                MenuButton(
                    title = "Mis Citas",
                    subtitle = "Consulta tus horarios",
                    icon = Icons.Default.Visibility,
                    backgroundColor = SoftWhite,
                    contentColor = DeepBlack,
                    borderColor = Gold,
                    onClick = onNavigateToAppointments
                )
            }
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PalePink)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterStart)
            ) {
                Surface(
                    color = Gold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DESTACADO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "IA Analysis",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DeepBlack
                    )
                )
                Text(
                    text = "Descubre el diseño ideal para tus cejas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBlack.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor.copy(alpha = 0.7f)
                    )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Podríamos añadir una flecha pequeña aquí si quisiéramos
        }
    }
}
