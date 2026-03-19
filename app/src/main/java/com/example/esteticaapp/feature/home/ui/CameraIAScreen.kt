package com.example.esteticaapp.feature.home.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.esteticaapp.core.network.NetworkUtils
import com.example.esteticaapp.ui.theme.VibrantPink
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@ExperimentalGetImage
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraIAScreen(onLockNavigation: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        CameraContent(context, onLockNavigation)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Se necesita acceso a la cámara para analizar tu rostro con IA.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        cameraPermissionState.launchPermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPink),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Conceder Permiso")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Abrir Configuración")
                }
            }
        }
    }
}

@ExperimentalGetImage
@Composable
fun CameraContent(context: Context, onLockNavigation: (Boolean) -> Unit = {}) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var resultJson by remember { mutableStateOf<JSONObject?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Bloquear navegación externa si está analizando o mostrando resultado
    LaunchedEffect(isAnalyzing, resultJson) {
        onLockNavigation(isAnalyzing || resultJson != null)
    }

    // Bloquear botón atrás físico
    if (isAnalyzing || resultJson != null) {
        BackHandler(enabled = true) {
            if (resultJson != null) {
                resultJson = null
            }
        }
    }

    // Estados para detección en tiempo real
    var isFaceDetected by remember { mutableStateOf(false) }
    var isEyesDetected by remember { mutableStateOf(false) }

    val feedbackMessages = listOf(
        "Analizando rasgos oculares...",
        "Calculando proporciones...",
        "Diseñando tu mirada ideal...",
        "Procesando diagnóstico..."
    )
    var currentFeedbackMessage by remember { mutableStateOf(feedbackMessages[0]) }

    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            var index = 0
            while (isAnalyzing) {
                currentFeedbackMessage = feedbackMessages[index % feedbackMessages.size]
                index++
                delay(2000)
            }
        }
    }

    val generativeModel = remember {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    val accurateFaceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build()
        FaceDetection.getClient(options)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            faceDetector.process(image)
                                .addOnSuccessListener { faces ->
                                    isFaceDetected = faces.isNotEmpty()
                                    if (faces.isNotEmpty()) {
                                        val face = faces[0]
                                        isEyesDetected = face.getLandmark(FaceLandmark.LEFT_EYE) != null &&
                                                face.getLandmark(FaceLandmark.RIGHT_EYE) != null
                                    } else {
                                        isEyesDetected = false
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraX", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        FaceGuideOverlay(isFaceDetected)

        if (isAnalyzing) {
            ScanningOverlay(currentFeedbackMessage)
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Análisis de Mirada IA",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetectionIndicator("Rostro", isFaceDetected)
                Spacer(modifier = Modifier.width(20.dp))
                DetectionIndicator("Ojos", isEyesDetected)
            }
        }

        if (!isAnalyzing && resultJson == null) {
            CaptureButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                isEnabled = isFaceDetected && isEyesDetected,
                onClick = {
                    if (!NetworkUtils.isOnline(context)) {
                        Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                        return@CaptureButton
                    }
                    isAnalyzing = true
                    errorMessage = null
                    val executor = Executors.newSingleThreadExecutor()
                    imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toBitmap()
                            image.close()
                            coroutineScope.launch {
                                procesarAnalisisHibrido(
                                    accurateFaceDetector,
                                    generativeModel,
                                    bitmap
                                ) { result, error ->
                                    isAnalyzing = false
                                    resultJson = result
                                    errorMessage = error
                                }
                            }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            isAnalyzing = false
                            errorMessage = "Error al capturar imagen"
                        }
                    })
                }
            )
        }

        if (resultJson != null) {
            ResultBottomSheet(resultJson!!) {
                resultJson = null
            }
        }

        if (errorMessage != null) {
            ErrorOverlay(errorMessage!!) {
                errorMessage = null
            }
        }
    }
}

@Composable
fun DetectionIndicator(label: String, isDetected: Boolean) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isDetected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f),
        animationSpec = tween(500), label = ""
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isDetected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = indicatorColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDetected) "$label detectado" else label,
            color = if (isDetected) Color.White else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isDetected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FaceGuideOverlay(isFaceDetected: Boolean) {
    val borderColor by animateColorAsState(
        targetValue = if (isFaceDetected) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
        animationSpec = tween(500), label = ""
    )

    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
        val width = size.width * 0.7f
        val height = size.height * 0.45f
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2.5f

        val ovalPath = Path().apply {
            addOval(Rect(left, top, left + width, top + height))
        }
        
        drawRect(Color.Black.copy(alpha = 0.4f))
        
        drawPath(
            path = ovalPath,
            color = Color.Transparent,
            blendMode = BlendMode.Clear
        )

        drawOval(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CaptureButton(modifier: Modifier, isEnabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEnabled) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isEnabled) VibrantPink else Color.Gray.copy(alpha = 0.3f),
        label = ""
    )

    Box(
        modifier = modifier
            .size(90.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(3.dp, if (isEnabled) Color.White else Color.White.copy(alpha = 0.3f))
        ) {}

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .clickable(enabled = isEnabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Face, 
                contentDescription = null, 
                tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f), 
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Ajuste necesario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
fun ScanningOverlay(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "line"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(10.dp).background(Color.Black.copy(alpha = 0.4f)))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * (0.25f + progress * 0.5f)
            
            drawLine(
                color = VibrantPink,
                start = Offset(size.width * 0.15f, y),
                end = Offset(size.width * 0.85f, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to VibrantPink.copy(alpha = 0.3f),
                    1f to Color.Transparent,
                    startY = y - 40.dp.toPx(),
                    endY = y + 40.dp.toPx()
                ),
                topLeft = Offset(size.width * 0.15f, y - 40.dp.toPx()),
                size = Size(size.width * 0.7f, 80.dp.toPx())
            )

            val points = listOf(
                Offset(size.width * 0.4f, size.height * 0.4f),
                Offset(size.width * 0.6f, size.height * 0.4f),
                Offset(size.width * 0.5f, size.height * 0.5f),
                Offset(size.width * 0.45f, size.height * 0.6f),
                Offset(size.width * 0.55f, size.height * 0.6f)
            )
            
            points.forEach { point ->
                drawCircle(
                    color = VibrantPink,
                    radius = (2.dp.toPx() * (1f + progress)),
                    center = point,
                    alpha = 0.8f
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(top = 280.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "POTENCIADO POR IA",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun ResultBottomSheet(json: JSONObject, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth(0.95f)
                    .padding(16.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tu Diagnóstico IA",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(VibrantPink, Color(0xFFFF80AB))
                                ), 
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "ESTILO RECOMENDADO", 
                                style = MaterialTheme.typography.labelLarge, 
                                color = Color.White.copy(alpha = 0.9f), 
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                json.optString("recomendacion"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Ideal para tu mirada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FORMA DETECTADA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F5F5)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(json.optString("formaOjo"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }

                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink.copy(alpha = 0.3f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISEÑO IDEAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = VibrantPink.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(json.optString("recomendacion"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = VibrantPink)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F8F8), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("¿Por qué te favorece?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            json.optString("explicacion"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nota de IA (Punto solicitado)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Aunque esto es un análisis basado en IA, puedes mostrar este análisis a la aplicadora para que lo tome en cuenta.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1976D2),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPink),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Finalizar análisis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Resultados guardados en tu perfil",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ResultBlock(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = VibrantPink.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

private suspend fun procesarAnalisisHibrido(
    faceDetector: com.google.mlkit.vision.face.FaceDetector,
    generativeModel: GenerativeModel,
    inputBitmap: Bitmap,
    onResult: (JSONObject?, String?) -> Unit
) {
    var finalBitmap = inputBitmap
    var faces: List<com.google.mlkit.vision.face.Face> = emptyList()
    var successfulRotation = 0

    try {
        val rotations = listOf(0, 90, 180, 270)
        
        for (rotation in rotations) {
            val inputImage = InputImage.fromBitmap(inputBitmap, rotation)
            faces = faceDetector.process(inputImage).await()
            
            if (faces.isNotEmpty()) {
                successfulRotation = rotation
                break
            }
        }
        
        if (faces.isEmpty()) {
            onResult(null, "No detectamos tu rostro claramente. Asegúrate de estar en un lugar iluminado y mirar de frente.")
            return
        }

        if (successfulRotation != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(successfulRotation.toFloat())
            finalBitmap = Bitmap.createBitmap(
                inputBitmap, 0, 0, 
                inputBitmap.width, inputBitmap.height, 
                matrix, true
            )
        }

        val face = faces[0]
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        
        if (leftEye == null || rightEye == null) {
            onResult(null, "Tus ojos no son visibles. Por favor, retira anteojos o mejora la luz.")
            return
        }

        val biometriaStr = extraerDatosBiometricos(face)
        
        val prompt = """
            Actúa como un experto en estética y visagismo facial de alta gama.
            Analiza la imagen adjunta y utiliza los datos biométricos extraídos.
            
            Tu objetivo es realizar un diagnóstico para extensiones de pestañas.
            IMPORTANTE: Tu respuesta debe ser profesional, experta y cercana. 
            NUNCA menciones coordenadas (X, Y), números técnicos de ML o píxeles. 
            Háblale a la clienta sobre la armonía de su mirada, la forma de sus ojos y cómo resaltarlos.
            
            Responde estrictamente en formato JSON:
            {
              "formaOjo": "Nombre corto (ej. Almendrado, Encapotado)",
              "tipoCejas": "Descripción estética",
              "simetria": "Evaluación visual (ej. Excelente balance natural)",
              "recomendacion": "Nombre del estilo ideal (ej. Cat Eye suave, Dolly Look)",
              "explicacion": "Explicación profesional de por qué este estilo es el ideal para ella, basada en sus rasgos únicos. Máximo 2 párrafos cortos."
            }
        """.trimIndent()

        val inputContent = content {
            image(finalBitmap)
            text(prompt)
        }

        val response = generativeModel.generateContent(inputContent)
        val responseText = response.text ?: ""
        val jsonStr = responseText.substringAfter("```json").substringBefore("```").trim()
        val finalJson = JSONObject(jsonStr.ifEmpty { responseText })

        val user = Firebase.auth.currentUser
        if (user != null) {
            try {
                val db = Firebase.firestore
                val now = Timestamp.now()
                val diagnosticoData = mutableMapOf<String, Any>()
                finalJson.keys().forEach { key ->
                    diagnosticoData[key] = finalJson.get(key)
                }
                diagnosticoData["timestamp"] = now
                diagnosticoData["biometria_mlkit"] = biometriaStr
                
                // Guardar en el historial
                db.collection("clientes").document(user.uid)
                    .collection("analysis_history")
                    .add(diagnosticoData)
                    .await()

                // Guardar el último análisis directo en el perfil para acceso rápido
                val lastAnalysisSummary = mapOf(
                    "recommended_style" to finalJson.optString("recomendacion"),
                    "eye_shape" to finalJson.optString("formaOjo"),
                    "date" to now,
                    "explanation" to finalJson.optString("explicacion")
                )
                db.collection("clientes").document(user.uid)
                    .set(mapOf("last_analysis" to lastAnalysisSummary), SetOptions.merge())
                    .await()

            } catch (e: Exception) {
                Log.e("HybridAnalysis", "Error saving: ${e.message}")
            }
        }

        onResult(finalJson, null)

    } catch (e: Exception) {
        Log.e("HybridAnalysis", "Error general: ${e.message}")
        onResult(null, "Error en el análisis. Intenta de nuevo.")
    }
}

private fun extraerDatosBiometricos(face: com.google.mlkit.vision.face.Face): String {
    val sb = StringBuilder()
    face.getLandmark(FaceLandmark.LEFT_EYE)?.let { sb.append("Ojo Izquierdo: [${it.position.x}, ${it.position.y}]\n") }
    face.getLandmark(FaceLandmark.RIGHT_EYE)?.let { sb.append("Ojo Derecho: [${it.position.x}, ${it.position.y}]\n") }
    face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.let { contour ->
        sb.append("Ceja Izquierda: ${contour.points.take(5).joinToString { "(${it.x.toInt()},${it.y.toInt()})" }}\n")
    }
    face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.let { contour ->
        sb.append("Ceja Derecha: ${contour.points.take(5).joinToString { "(${it.x.toInt()},${it.y.toInt()})" }}\n")
    }
    return sb.toString()
}
