/*
 * AdminGalleryScreen permite al administrador gestionar la galeria de servicios.
 * Carga items desde Firestore, los muestra por categorias fijas y permite
 * crear/editar cada servicio mediante un dialogo con subida de imagen a Cloudinary.
 * Incluye guardado en base de datos y feedback visual con snackbar/estados de carga.
 */

package com.example.esteticaapp.feature.admin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.esteticaapp.core.model.GaleriaItem
import com.example.esteticaapp.ui.theme.PrimaryPink
import com.example.esteticaapp.ui.theme.SoftRose
import com.example.esteticaapp.ui.theme.TextPrimary
import com.example.esteticaapp.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGalleryScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val adminCategories = listOf("Lash Lifting", "Extensión de Pestañas", "Diseño de Cejas")
    val allowedCategories = adminCategories
    var galleryItems by remember { mutableStateOf<List<GaleriaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf<GaleriaItem?>(null) }

    // Fetch services
    LaunchedEffect(Unit) {
        db.collection("gallery_services").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                isLoading = false
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GaleriaItem::class.java)?.copy(id = doc.id)
            }.filter { item ->
                allowedCategories.any { category -> item.category.equals(category, ignoreCase = true) }
            }
            galleryItems = items
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFFF9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Panel de Gestión",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Regresar",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp, 
                            contentDescription = "Cerrar Sesión",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    text = "Administra las imágenes de tus servicios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showEditDialog = GaleriaItem(category = adminCategories.first()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Subir nueva imagen", fontWeight = FontWeight.Bold)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPink)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(adminCategories) { category ->
                        val item = galleryItems.find { it.category == category }
                        AdminServiceCard(
                            category = category,
                            item = item,
                            onEdit = { showEditDialog = it ?: GaleriaItem(category = category) }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog != null) {
        AdminServiceManagementDialog(
            item = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onSave = { newItem ->
                val finalItem = newItem.copy(updatedAt = System.currentTimeMillis())
                db.collection("gallery_services")
                    .run {
                        if (finalItem.id.isEmpty()) add(finalItem)
                        else document(finalItem.id).set(finalItem, SetOptions.merge())
                    }
                    .addOnSuccessListener {
                        showEditDialog = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Servicio actualizado correctamente")
                        }
                    }
                    .addOnFailureListener {
                         scope.launch {
                            snackbarHostState.showSnackbar("Error al guardar en la base de datos")
                        }
                    }
            }
        )
    }
}

@Composable
private fun AdminServiceCard(
    category: String,
    item: GaleriaItem?,
    onEdit: (GaleriaItem?) -> Unit
) {
    val isConfigured = item != null && item.imageUrl.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(item) },
        shape = RoundedCornerShape(20.dp),
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
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isConfigured) Color.Transparent else SoftRose),
                contentAlignment = Alignment.Center
            ) {
                if (isConfigured && item != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.ImageNotSupported, null, tint = PrimaryPink)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(category, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConfigured) Color(0xFF4CAF50) else Color.LightGray)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isConfigured) "Configurado" else "Sin configurar",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isConfigured) Color(0xFF4CAF50) else Color.Gray
                    )
                }

                if (isConfigured && item != null) {
                    Text(
                        "Actualizado: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.updatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            IconButton(
                onClick = { onEdit(item) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = SoftRose.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = PrimaryPink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdminServiceManagementDialog(
    item: GaleriaItem,
    onDismiss: () -> Unit,
    onSave: (GaleriaItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(item.title) }
    var imageUrl by remember { mutableStateOf(item.imageUrl) }
    var category by remember { mutableStateOf(item.category) }
    var description by remember { mutableStateOf(item.description) }
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Lash Lifting", "Extensión de Pestañas", "Diseño de Cejas")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch(Dispatchers.IO) {
                isUploading = true
                uploadError = null
                val imageData = compressImage(context, selectedUri)
                if (imageData == null) {
                    isUploading = false
                    uploadError = "Error al procesar la imagen"
                    return@launch
                }
                if (imageData.size > 10 * 1024 * 1024) { 
                    isUploading = false
                    uploadError = "La imagen es demasiado pesada incluso comprimida. Máx: 10MB"
                    return@launch
                }
                MediaManager.get().upload(imageData)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            imageUrl = resultData?.get("secure_url") as? String ?: ""
                            isUploading = false
                        }
                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            isUploading = false
                            uploadError = "Error Cloudinary: ${error?.description}"
                        }
                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                    }).dispatch()
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryPink, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Gestionar Servicio",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Vista previa del trabajo", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray.copy(alpha = 0.1f))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable { if (!isUploading) launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryPink)
                                Text("Subiendo imagen...", color = PrimaryPink, style = MaterialTheme.typography.labelSmall)
                            }
                        } else if (imageUrl.isEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = PrimaryPink, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Toca para subir una foto", color = PrimaryPink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    uploadError?.let {
                        Text(it, color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    Text(
                        "Formatos permitidos: JPG / PNG • Máx: 10MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text("Categoría del servicio", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = category == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPink,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre del servicio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }
                        
                        Button(
                            onClick = { 
                                isSaving = true
                                onSave(item.copy(title = title, imageUrl = imageUrl, category = category, description = description)) 
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isUploading && imageUrl.isNotEmpty() && !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("Guardar")
                            }
                        }
                    }
                }
            }
        }
    )
}

private fun compressImage(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}
