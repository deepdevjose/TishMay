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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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

    LaunchedEffect(isAnalyzing, resultJson) {
        onLockNavigation(isAnalyzing || resultJson != null)
    }

    if (isAnalyzing || resultJson != null) {
        BackHandler(enabled = true) {
            if (resultJson != null) {
                resultJson = null
            }
        }
    }

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
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "iconPulse"
                )
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = VibrantPink,
                    modifier = Modifier.size(24.dp).graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Análisis de Mirada IA",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetectionIndicator("Rostro", isFaceDetected)
                Spacer(modifier = Modifier.width(24.dp))
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
        targetValue = if (isDetected) Color(0xFF00E676) else Color.White.copy(alpha = 0.3f),
        animationSpec = tween(600), label = "color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isDetected) 1.1f else 1.0f,
        animationSpec = tween(400, easing = LinearEasing), label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(indicatorColor, CircleShape)
                .border(2.dp, if (isDetected) Color.White.copy(alpha = 0.5f) else Color.Transparent, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isDetected) Color.White else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isDetected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
fun FaceGuideOverlay(isFaceDetected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "guide")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFaceDetected) VibrantPink else Color.White,
        animationSpec = tween(500), label = "color"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width * 0.72f
        val height = size.height * 0.48f
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2.6f

        val ovalPath = Path().apply {
            addOval(Rect(left, top, left + width, top + height))
        }

        // Fondo oscuro con agujero
        drawPath(
            path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addPath(ovalPath)
                fillType = PathFillType.EvenOdd
            },
            color = Color.Black.copy(alpha = 0.5f)
        )

        // Borde elegante
        drawOval(
            color = borderColor.copy(alpha = borderAlpha),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Esquinas decorativas
        val cornerSize = 40.dp.toPx()
        val padding = 10.dp.toPx()
        
        // Arriba Izquierda
        drawPath(
            path = Path().apply {
                moveTo(left - padding, top - padding + cornerSize)
                lineTo(left - padding, top - padding)
                lineTo(left - padding + cornerSize, top - padding)
            },
            color = borderColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Arriba Derecha
        drawPath(
            path = Path().apply {
                moveTo(left + width + padding - cornerSize, top - padding)
                lineTo(left + width + padding, top - padding)
                lineTo(left + width + padding, top - padding + cornerSize)
            },
            color = borderColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Abajo Izquierda
        drawPath(
            path = Path().apply {
                moveTo(left - padding, top + height + padding - cornerSize)
                lineTo(left - padding, top + height + padding)
                lineTo(left - padding + cornerSize, top + height + padding)
            },
            color = borderColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Abajo Derecha
        drawPath(
            path = Path().apply {
                moveTo(left + width + padding - cornerSize, top + height + padding)
                lineTo(left + width + padding, top + height + padding)
                lineTo(left + width + padding, top + height + padding - cornerSize)
            },
            color = borderColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CaptureButton(modifier: Modifier, isEnabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "capture")
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEnabled) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "outer"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "press")

    Box(
        modifier = modifier
            .size(100.dp)
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale),
        contentAlignment = Alignment.Center
    ) {
        // Anillo exterior animado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = outerScale, scaleY = outerScale)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(VibrantPink.copy(0.2f), VibrantPink, VibrantPink.copy(0.2f))
                    ),
                    shape = CircleShape
                )
        )

        // Botón principal
        Surface(
            modifier = Modifier
                .size(76.dp)
                .clickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = CircleShape,
            color = if (isEnabled) VibrantPink else Color.White.copy(alpha = 0.2f),
            shadowElevation = if (isEnabled) 12.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "¡Casi listo!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPink),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Intentar de nuevo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "line"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Blur suave de fondo
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * (0.25f + progress * 0.5f)

            // Línea de escaneo láser
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, VibrantPink, Color.Transparent)
                ),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Resplandor del láser
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, VibrantPink.copy(alpha = 0.4f), Color.Transparent),
                    startY = y - 60.dp.toPx(),
                    endY = y + 60.dp.toPx()
                ),
                topLeft = Offset(0f, y - 60.dp.toPx()),
                size = Size(size.width, 120.dp.toPx())
            )
            
            // Puntos de biométrica "detectados"
            val points = listOf(
                Offset(size.width * 0.35f, size.height * 0.38f),
                Offset(size.width * 0.65f, size.height * 0.38f),
                Offset(size.width * 0.5f, size.height * 0.48f),
                Offset(size.width * 0.4f, size.height * 0.58f),
                Offset(size.width * 0.6f, size.height * 0.58f)
            )

            points.forEach { point ->
                val distance = kotlin.math.abs(point.y - y)
                val alpha = (1f - (distance / (size.height * 0.1f))).coerceIn(0f, 1f)
                
                if (alpha > 0) {
                    drawCircle(
                        color = VibrantPink,
                        radius = 4.dp.toPx() * alpha,
                        center = point,
                        alpha = alpha
                    )
                    drawCircle(
                        color = VibrantPink.copy(alpha = 0.3f),
                        radius = 12.dp.toPx() * alpha,
                        center = point,
                        alpha = alpha
                    )
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(top = 260.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(VibrantPink.copy(alpha = 0.1f), CircleShape).padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VISAGISMO DIGITAL",
                    color = VibrantPink,
                    fontWeight = FontWeight.Black,
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

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
