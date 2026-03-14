package com.example.esteticaapp.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.esteticaapp.NetworkUtils
import com.example.esteticaapp.ui.theme.VibrantPink
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceContour
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraIAScreen() {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (cameraPermissionState.status.isGranted) {
        CameraContent(context)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se necesita permiso de cámara para usar la IA", color = Color.Gray)
        }
    }
}

@Composable
fun CameraContent(context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var resultJson by remember { mutableStateOf<JSONObject?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val feedbackMessages = listOf(
        "Analizando simetría...",
        "Buscando el mejor diseño para ti...",
        "Escaneando mirada...",
        "Calculando proporciones...",
        "Identificando forma de ojo...",
        "Procesando rasgos faciales..."
    )
    var currentFeedbackMessage by remember { mutableStateOf(feedbackMessages[0]) }

    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            while (isAnalyzing) {
                currentFeedbackMessage = feedbackMessages.random()
                delay(2000)
            }
        }
    }

    // Configuración de Gemini con Firebase AI Logic
    val generativeModel = remember {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    // Configuración de ML Kit Face Detector para Landmarks y Contours
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f) // Cambiado de 0.15f a 0.1f para detectar rostros más lejanos
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

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraX", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isAnalyzing) {
            ScanningOverlay(currentFeedbackMessage)
        }

        if (!isAnalyzing && resultJson == null) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VibrantPink, modifier = Modifier.size(40.dp))
                Text(
                    "Análisis de Mirada IA",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Centra tus ojos en la cámara",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!isAnalyzing && resultJson == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(VibrantPink)
                    .clickable {
                        if (!NetworkUtils.isOnline(context)) {
                            Toast.makeText(context, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
                            return@clickable
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
                                        faceDetector,
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
                                errorMessage = "Error al capturar: ${exception.message}"
                            }
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.White)
                ) {}
            }
        }

        if (resultJson != null) {
            ResultCard(resultJson!!) {
                resultJson = null
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Error de Análisis", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(errorMessage!!, color = Color.White)
                    Button(
                        onClick = { errorMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
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

    Log.d("FaceDetection", "Iniciando análisis. Tamaño Bitmap: ${inputBitmap.width}x${inputBitmap.height}")

    try {
        // Intentar detectar rostros en diferentes rotaciones (0, 90, 180, 270)
        // A veces la cámara frontal entrega la imagen rotada sin avisar
        val rotations = listOf(0, 90, 180, 270)
        
        for (rotation in rotations) {
            val inputImage = InputImage.fromBitmap(inputBitmap, rotation)
            faces = faceDetector.process(inputImage).await()
            
            if (faces.isNotEmpty()) {
                Log.d("FaceDetection", "Rostro detectado exitosamente con rotación: $rotation grados. Cantidad: ${faces.size}")
                successfulRotation = rotation
                break
            } else {
                Log.d("FaceDetection", "Intento fallido con rotación $rotation grados")
            }
        }
        
        if (faces.isEmpty()) {
            Log.d("FaceDetection", "No se detectaron rostros en ninguna rotación.")
            onResult(null, "No se detectó ningún rostro. Intenta acercarte un poco más o mejorar la luz.")
            return
        }

        // Si se detectó con una rotación específica, rotamos el bitmap para que Gemini lo reciba derecho
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
        
        // Manejo de error de 'Falta de iluminación' usando ML Kit
        // Si no se detectan los ojos básicos, reportamos falta de luz
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        
        if (leftEye == null || rightEye == null) {
            onResult(null, "Falta de iluminación. Por favor, intenta en un lugar con mejor luz.")
            return
        }

        // Extraer posiciones de ojos y cejas (Landmarks y Contours)
        val biometriaStr = extraerDatosBiometricos(face)
        
        val prompt = """
            Actúa como un experto en estética y visagismo facial.
            Analiza la imagen adjunta y utiliza los siguientes datos biométricos precisos extraídos con ML Kit:
            $biometriaStr
            
            Tu objetivo es realizar un diagnóstico para extensiones de pestañas.
            Responde estrictamente en formato JSON con la siguiente estructura:
            {
              "formaOjo": "descripción de la forma",
              "tipoCejas": "descripción de las cejas",
              "simetria": "descripción de la simetría facial",
              "recomendacion": "estilo de pestañas ideal (Dolly, Cat Eye, etc.)",
              "explicacion": "razón técnica de la elección"
            }
        """.trimIndent()

        val inputContent = content {
            image(finalBitmap)
            text(prompt)
        }

        val response = generativeModel.generateContent(inputContent)
        val responseText = response.text ?: ""
        val jsonStr = responseText.substringAfter("```json").substringBefore("```").trim()
        val finalJson = JSONObject(if (jsonStr.isEmpty()) responseText else jsonStr)

        // Lógica de Firebase: Al recibir la respuesta, guardar el objeto JSON completo en Firestore
        // Colección: clientes/{uid}/diagnosticos
        val user = Firebase.auth.currentUser
        if (user != null) {
            // Envolvemos el guardado en un try-catch independiente para que un error de permisos
            // no impida mostrar el resultado al usuario.
            try {
                val db = Firebase.firestore
                val diagnosticoData = mutableMapOf<String, Any>()
                finalJson.keys().forEach { key ->
                    diagnosticoData[key] = finalJson.get(key)
                }
                diagnosticoData["timestamp"] = Timestamp.now()
                diagnosticoData["biometria_mlkit"] = biometriaStr
                
                db.collection("clientes").document(user.uid)
                    .collection("diagnosticos")
                    .add(diagnosticoData)
                    .await()
            } catch (e: Exception) {
                Log.e("HybridAnalysis", "Error al guardar en Firestore (probablemente reglas de seguridad): ${e.message}")
                // No detenemos el flujo, el usuario verá su resultado de todas formas
            }
        }

        onResult(finalJson, null)

    } catch (e: Exception) {
        Log.e("HybridAnalysis", "Error al procesar la imagen: ${e.message}", e)
        onResult(null, "Error en el procesamiento: ${e.message}")
    }
}

private fun extraerDatosBiometricos(face: com.google.mlkit.vision.face.Face): String {
    val sb = StringBuilder()
    
    // Posiciones de ojos (Landmarks)
    face.getLandmark(FaceLandmark.LEFT_EYE)?.let { sb.append("Ojo Izquierdo: [${it.position.x}, ${it.position.y}]\n") }
    face.getLandmark(FaceLandmark.RIGHT_EYE)?.let { sb.append("Ojo Derecho: [${it.position.x}, ${it.position.y}]\n") }
    
    // Contornos de cejas (Contours)
    face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.let { contour ->
        sb.append("Ceja Izquierda (superior): ${contour.points.take(10).joinToString { "(${it.x.toInt()},${it.y.toInt()})" }}\n")
    }
    face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.let { contour ->
        sb.append("Ceja Derecha (superior): ${contour.points.take(10).joinToString { "(${it.x.toInt()},${it.y.toInt()})" }}\n")
    }

    // Clasificaciones adicionales (útiles para el contexto)
    sb.append("Ojo Izquierdo Abierto Prob: ${face.leftEyeOpenProbability}\n")
    sb.append("Ojo Derecho Abierto Prob: ${face.rightEyeOpenProbability}\n")

    return sb.toString()
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * (0.25f + progress * 0.5f)
            drawLine(
                color = VibrantPink,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to VibrantPink.copy(alpha = 0.4f),
                    1f to Color.Transparent,
                    startY = y - 50.dp.toPx(),
                    endY = y + 50.dp.toPx()
                ),
                topLeft = Offset(0f, y - 50.dp.toPx()),
                size = Size(size.width, 100.dp.toPx())
            )
        }
        Text(
            text = message.uppercase(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 180.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            color = VibrantPink,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ResultCard(json: JSONObject, onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Diagnóstico Estético",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = VibrantPink
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                ResultRow("Forma de Ojo", json.optString("formaOjo"))
                ResultRow("Tipo de Cejas", json.optString("tipoCejas"))
                ResultRow("Simetría", json.optString("simetria"))
                ResultRow("Recomendación", json.optString("recomendacion"))
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    json.optString("explicacion"),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPink)
                ) {
                    Text("Cerrar y Guardar")
                }
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Text(value, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
