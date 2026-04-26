package com.commpass357.orbitx.ui

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commpass357.orbitx.sim.BodyState
import com.commpass357.orbitx.sim.BodyType
import com.commpass357.orbitx.sim.SpawnPreset
import com.commpass357.orbitx.sim.Vector2
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private const val VelocityHandleScaleDays = 45.0

private data class OrbitCamera(
    val offset: Offset = Offset.Zero,
    val zoom: Float = 28f
)

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
fun OrbitGameScreen(viewModel: OrbitViewModel = viewModel()) {
    val state = viewModel.uiState
    var camera by remember { mutableStateOf(OrbitCamera()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(viewModel) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrame != 0L) {
                    viewModel.onFrame((now - lastFrame) / 1_000_000_000.0)
                }
                lastFrame = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OrbitCanvas(
            state = state,
            camera = camera,
            onCameraChange = { camera = it },
            onSizeChange = { canvasSize = it },
            onSelectAt = { point ->
                val body = findBodyAt(point, state.snapshot.bodies, camera, canvasSize)
                viewModel.selectBody(body?.id)
            },
            onMoveSelected = viewModel::moveSelected,
            onVelocityHandle = { handleWorld ->
                viewModel.setSelectedVelocityFromHandle(handleWorld, VelocityHandleScaleDays)
            }
        )

        TopCommandBar(
            state = state,
            onTogglePlay = viewModel::togglePlay,
            onReset = viewModel::reset,
            onOpenPalette = viewModel::togglePalette,
            onZoomIn = { camera = camera.copy(zoom = (camera.zoom * 1.18f).coerceIn(8f, 260f)) },
            onZoomOut = { camera = camera.copy(zoom = (camera.zoom / 1.18f).coerceIn(8f, 260f)) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ToolDock(
            state = state,
            onTool = viewModel::setTool,
            onTrails = viewModel::toggleTrails,
            onPrediction = viewModel::togglePrediction,
            onTimeScale = viewModel::setTimeScale,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        EventFeed(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 86.dp)
        )

        AnimatedVisibility(
            visible = state.selectedBody != null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        ) {
            SelectedBodyInspector(
                body = state.selectedBody,
                onClose = { viewModel.selectBody(null) },
                onDelete = viewModel::deleteSelected,
                onLock = viewModel::toggleSelectedLock,
                onMassDown = { viewModel.scaleSelectedMass(0.72) },
                onMassUp = { viewModel.scaleSelectedMass(1.38) },
                onVelocityDown = { viewModel.nudgeSelectedVelocity(-1.0) },
                onVelocityUp = { viewModel.nudgeSelectedVelocity(1.0) },
                onCircularize = viewModel::circularizeSelected
            )
        }

        AnimatedVisibility(
            visible = state.paletteOpen,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            SpawnPalette(
                presets = viewModel.spawnPresets,
                onClose = viewModel::closePalette,
                onSpawn = { preset ->
                    val center = screenToWorld(
                        Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                        canvasSize,
                        camera
                    )
                    viewModel.spawnPresetAt(preset, center)
                }
            )
        }
    }
}

@Composable
private fun OrbitCanvas(
    state: OrbitUiState,
    camera: OrbitCamera,
    onCameraChange: (OrbitCamera) -> Unit,
    onSizeChange: (IntSize) -> Unit,
    onSelectAt: (Offset) -> Unit,
    onMoveSelected: (Vector2) -> Unit,
    onVelocityHandle: (Vector2) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val stars = remember {
        val random = Random(4567)
        List(220) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = random.nextDouble(0.6, 1.8).toFloat(),
                alpha = random.nextDouble(0.22, 0.9).toFloat()
            )
        }
    }

    val tapModifier = Modifier.pointerInput(state.snapshot.bodies, camera, size) {
        detectTapGestures { offset -> onSelectAt(offset) }
    }

    val gestureModifier = when (state.activeTool) {
        GameTool.Navigate -> Modifier.pointerInput(camera, size) {
            detectTransformGestures { _, pan, zoomChange, _ ->
                onCameraChange(
                    camera.copy(
                        offset = camera.offset + pan,
                        zoom = (camera.zoom * zoomChange).coerceIn(8f, 260f)
                    )
                )
            }
        }

        GameTool.Move -> Modifier.pointerInput(state.selectedId, camera, size) {
            detectDragGestures { change, _ ->
                onMoveSelected(screenToWorld(change.position, size, camera))
            }
        }

        GameTool.Velocity -> Modifier.pointerInput(state.selectedId, camera, size) {
            detectDragGestures { change, _ ->
                onVelocityHandle(screenToWorld(change.position, size, camera))
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                size = it
                onSizeChange(it)
            }
            .then(tapModifier)
            .then(gestureModifier)
    ) {
        drawRect(Color(0xFF050711))
        drawStars(stars)
        drawPrediction(state, camera, size)
        if (state.showTrails) drawTrails(state.snapshot.bodies, state.selectedId, camera, size)
        drawBodies(state.snapshot.bodies, state.selectedId, camera, size)
        drawVelocityHandle(state.selectedBody, state.activeTool, camera, size)
    }
}

private fun DrawScope.drawStars(stars: List<Star>) {
    stars.forEach { star ->
        drawCircle(
            color = Color.White.copy(alpha = star.alpha),
            radius = star.radius,
            center = Offset(star.x * size.width, star.y * size.height)
        )
    }
}

private fun DrawScope.drawPrediction(state: OrbitUiState, camera: OrbitCamera, canvasSize: IntSize) {
    if (!state.showPrediction || state.predictionPaths.isEmpty()) return
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
    state.predictionPaths.forEach { (id, points) ->
        if (points.size < 2) return@forEach
        val selected = id == state.selectedId
        val color = if (selected) Color(0xFFFFF2A8) else Color(0xFF7ED7FF).copy(alpha = 0.28f)
        for (index in 0 until points.lastIndex) {
            drawLine(
                color = color,
                start = worldToScreen(points[index], canvasSize, camera),
                end = worldToScreen(points[index + 1], canvasSize, camera),
                strokeWidth = if (selected) 2.4f else 1.1f,
                cap = StrokeCap.Round,
                pathEffect = dash
            )
        }
    }
}

private fun DrawScope.drawTrails(
    bodies: List<BodyState>,
    selectedId: Long?,
    camera: OrbitCamera,
    canvasSize: IntSize
) {
    bodies.forEach { body ->
        if (body.trail.size < 2) return@forEach
        if ((body.type == BodyType.Asteroid || body.type == BodyType.Fragment) && body.id != selectedId) return@forEach
        val color = Color(body.colorArgb).copy(alpha = if (body.type == BodyType.Asteroid) 0.18f else 0.34f)
        val trail = body.trail.takeLast(72)
        for (index in 0 until trail.lastIndex) {
            val alpha = ((index + 1f) / trail.size).coerceIn(0.18f, 1f)
            drawLine(
                color = color.copy(alpha = color.alpha * alpha),
                start = worldToScreen(trail[index], canvasSize, camera),
                end = worldToScreen(trail[index + 1], canvasSize, camera),
                strokeWidth = if (body.type == BodyType.Asteroid) 0.8f else 1.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawBodies(
    bodies: List<BodyState>,
    selectedId: Long?,
    camera: OrbitCamera,
    canvasSize: IntSize
) {
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13f
    }
    bodies.sortedBy { it.mass }.forEach { body ->
        val center = worldToScreen(body.position, canvasSize, camera)
        val radius = screenRadius(body, camera)
        val color = Color(body.colorArgb)
        if (body.type == BodyType.Comet) {
            val tail = body.velocity.normalized() * -0.32
            drawLine(
                color = Color(0xFFB8F7FF).copy(alpha = 0.45f),
                start = center,
                end = worldToScreen(body.position + tail, canvasSize, camera),
                strokeWidth = max(2.5f, radius * 0.45f),
                cap = StrokeCap.Round
            )
        }
        if (body.name.startsWith("Saturn")) {
            drawOval(
                color = Color(0xFFEBD99B).copy(alpha = 0.45f),
                topLeft = center - Offset(radius * 1.9f, radius * 0.55f),
                size = Size(radius * 3.8f, radius * 1.1f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = max(1.2f, radius * 0.12f))
            )
        }
        drawCircle(
            color = color.copy(alpha = if (body.locked) 0.95f else 1f),
            radius = radius,
            center = center
        )
        if (body.id == selectedId) {
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = radius + 6f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
            )
        }
        if (body.type != BodyType.Asteroid && body.type != BodyType.Fragment && camera.zoom >= 16f) {
            drawContext.canvas.nativeCanvas.drawText(
                body.name,
                center.x + radius + 5f,
                center.y - radius - 3f,
                labelPaint
            )
        }
    }
}

private fun DrawScope.drawVelocityHandle(
    body: BodyState?,
    tool: GameTool,
    camera: OrbitCamera,
    canvasSize: IntSize
) {
    if (body == null || tool != GameTool.Velocity) return
    val start = worldToScreen(body.position, canvasSize, camera)
    val handleWorld = body.position + body.velocity * VelocityHandleScaleDays
    val end = worldToScreen(handleWorld, canvasSize, camera)
    drawLine(
        color = Color(0xFFFFF2A8),
        start = start,
        end = end,
        strokeWidth = 3.2f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFFFFF2A8), radius = 8f, center = end)
}

@Composable
private fun TopCommandBar(
    state: OrbitUiState,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    onOpenPalette: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "OrbitX",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Day ${state.snapshot.timeDays.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset")
            }
            IconButton(onClick = onZoomOut) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
            }
            IconButton(onClick = onZoomIn) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom in")
            }
            ElevatedButton(onClick = onOpenPalette) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Spawn")
            }
        }
    }
}

@Composable
private fun ToolDock(
    state: OrbitUiState,
    onTool: (GameTool) -> Unit,
    onTrails: () -> Unit,
    onPrediction: () -> Unit,
    onTimeScale: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            GameTool.entries.forEach { tool ->
                FilterChip(
                    selected = state.activeTool == tool,
                    onClick = { onTool(tool) },
                    label = { Text(tool.label, maxLines = 1) }
                )
            }
            AssistChip(onClick = onTrails, label = { Text(if (state.showTrails) "Trails on" else "Trails off") })
            AssistChip(
                onClick = onPrediction,
                label = { Text(if (state.showPrediction) "Prediction on" else "Prediction off") }
            )
            Text(
                text = "${state.timeScale.toInt()} d/s",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = state.timeScale.toFloat(),
                onValueChange = { onTimeScale(it.toDouble()) },
                valueRange = 1f..220f,
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

@Composable
private fun SelectedBodyInspector(
    body: BodyState?,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onLock: () -> Unit,
    onMassDown: () -> Unit,
    onMassUp: () -> Unit,
    onVelocityDown: () -> Unit,
    onVelocityUp: () -> Unit,
    onCircularize: () -> Unit
) {
    if (body == null) return
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.widthIn(min = 280.dp, max = 330.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(body.colorArgb), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = body.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = body.type.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            HorizontalDivider()
            MetricRow("Mass", body.mass.toMassLabel())
            MetricRow("Speed", "%.4f AU/day".format(body.velocity.length))
            MetricRow("Position", "%.2f, %.2f AU".format(body.position.x, body.position.y))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMassDown, modifier = Modifier.weight(1f)) { Text("Mass -") }
                Button(onClick = onMassUp, modifier = Modifier.weight(1f)) { Text("Mass +") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVelocityDown, modifier = Modifier.weight(1f)) { Text("Brake") }
                Button(onClick = onVelocityUp, modifier = Modifier.weight(1f)) { Text("Boost") }
            }
            Button(onClick = onCircularize, modifier = Modifier.fillMaxWidth()) {
                Text("Circularize")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLock, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (body.locked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (body.locked) "Unlock" else "Lock")
                }
                Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpawnPalette(
    presets: List<SpawnPreset>,
    onClose: () -> Unit,
    onSpawn: (SpawnPreset) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxHeight()
            .width(330.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "Spawn",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            HorizontalDivider()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                items(presets) { preset ->
                    SpawnPresetRow(preset = preset, onSpawn = { onSpawn(preset) })
                }
            }
        }
    }
}

@Composable
private fun SpawnPresetRow(preset: SpawnPreset, onSpawn: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Color(preset.colorArgb), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                preset.type.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Button(onClick = onSpawn) {
            Text("Add")
        }
    }
}

@Composable
private fun EventFeed(state: OrbitUiState, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.widthIn(max = 330.dp)
    ) {
        state.snapshot.events.take(3).forEach { event ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = event.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun findBodyAt(
    point: Offset,
    bodies: List<BodyState>,
    camera: OrbitCamera,
    canvasSize: IntSize
): BodyState? {
    return bodies
        .map { body ->
            val center = worldToScreen(body.position, canvasSize, camera)
            val hitRadius = max(screenRadius(body, camera) + 12f, 18f)
            body to (center - point).getDistance().takeIf { it <= hitRadius }
        }
        .filter { it.second != null }
        .minByOrNull { it.second ?: Float.MAX_VALUE }
        ?.first
}

private fun worldToScreen(position: Vector2, canvasSize: IntSize, camera: OrbitCamera): Offset {
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    return center + camera.offset + Offset(
        x = (position.x * camera.zoom).toFloat(),
        y = (-position.y * camera.zoom).toFloat()
    )
}

private fun screenToWorld(point: Offset, canvasSize: IntSize, camera: OrbitCamera): Vector2 {
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val shifted = point - center - camera.offset
    return Vector2(
        x = shifted.x.toDouble() / camera.zoom.toDouble(),
        y = -shifted.y.toDouble() / camera.zoom.toDouble()
    )
}

private fun screenRadius(body: BodyState, camera: OrbitCamera): Float {
    val base = (body.drawRadius * camera.zoom).toFloat()
    val minRadius = when (body.type) {
        BodyType.Star -> 13f
        BodyType.Planet -> 7f
        BodyType.Moon -> 5f
        BodyType.DwarfPlanet -> 5f
        BodyType.Asteroid -> 2.5f
        BodyType.Comet -> 4.5f
        BodyType.Probe -> 4.5f
        BodyType.Fragment -> 2.4f
        BodyType.Custom -> 6f
    }
    return min(max(base, minRadius), 34f)
}

private fun Double.toMassLabel(): String = when {
    this >= 0.5 -> "%.2f Suns".format(this)
    this >= 1.0e-3 -> "%.2f Jupiters".format(this / 9.545e-4)
    this >= 1.0e-6 -> "%.2f Earths".format(this / 3.003e-6)
    else -> "%.2e".format(this)
}
