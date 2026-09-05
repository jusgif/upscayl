package com.jusgif.upscayl

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { UpscaylTheme { UpscaylScreen() } }
    }

    @Composable
    private fun UpscaylScreen() {
        val scope = rememberCoroutineScope()
        val context = this
        var source by remember { mutableStateOf<Bitmap?>(null) }
        var result by remember { mutableStateOf<Bitmap?>(null) }
        var scale by remember { mutableIntStateOf(4) }
        var busy by remember { mutableStateOf(false) }
        var progress by remember { mutableIntStateOf(0) }
        var error by remember { mutableStateOf<String?>(null) }
        var showResult by remember { mutableStateOf(false) }
        val modelManager = remember { ModelManager(context) }
        val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
            if (uri != null) scope.launch {
                runCatching { ImageRepository.decode(context, uri) }
                    .onSuccess { source = it; result = null; showResult = false; error = null }
                    .onFailure { error = it.message ?: "Unable to open image" }
            }
        }

        Scaffold(containerColor = Color.Black) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Upscayl", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Text(if (modelManager.isReady()) "READY" else "MODEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().weight(1f).background(Color(0xFF050505), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val shown = if (showResult) result else source
                    if (shown == null) {
                        Button(onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, shape = RoundedCornerShape(14.dp)) {
                            Text("Select image")
                        }
                    } else {
                        ZoomImage(shown)
                        Surface(
                            Modifier.align(Alignment.TopCenter).padding(10.dp),
                            color = Color.Black.copy(alpha = .7f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(if (showResult) "UPSCALED" else "ORIGINAL", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (source != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = scale == 2, onClick = { scale = 2 }, label = { Text("2×") })
                        FilterChip(selected = scale == 4, onClick = { scale = 4 }, label = { Text("4×") })
                        if (result != null) FilterChip(selected = showResult, onClick = { showResult = !showResult }, label = { Text("Compare") })
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = !busy,
                        onClick = {
                            val input = source ?: return@Button
                            busy = true; error = null; progress = 0
                            scope.launch(Dispatchers.Default) {
                                runCatching {
                                    modelManager.ensureReady { progress = it }
                                    val x4 = NativeUpscaler.upscale(input, modelManager.path(), 256, true)
                                    if (scale == 2) Bitmap.createScaledBitmap(x4, input.width * 2, input.height * 2, true) else x4
                                }.onSuccess { up -> result = up; showResult = true }
                                    .onFailure { error = it.message ?: "Upscaling failed" }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (busy) "Upscaling $progress%" else "Upscale") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        enabled = result != null && !busy,
                        onClick = { scope.launch { runCatching { ImageRepository.save(context, result!!) }.onFailure { error = it.message } } },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Save to Gallery") }
                }
                AnimatedVisibility(error != null) { Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }

    @Composable
    private fun ZoomImage(bitmap: Bitmap) {
        var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
        var ox by remember(bitmap) { mutableFloatStateOf(0f) }
        var oy by remember(bitmap) { mutableFloatStateOf(0f) }
        Image(
            bitmap.asImageBitmap(),
            null,
            Modifier.fillMaxSize().padding(10.dp)
                .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = ox, translationY = oy)
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                        ox += pan.x; oy += pan.y
                    }
                }
        )
    }
}

@Composable
private fun UpscaylTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color.Black,
            surface = Color.Black
        ),
        content = content
    )
}
