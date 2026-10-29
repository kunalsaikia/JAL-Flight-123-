package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun FlightCanvas3D(
    uiState: FlightSimulationUiState,
    onOrbit: (deltaAzimuth: Float, deltaElevation: Float) -> Unit,
    onZoom: (zoomFactor: Float) -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flightState = uiState.flightState
    val cameraState = uiState.cameraState
    val terrainMode = uiState.terrainMode

    // Load satellite texture map and grayscale heightmap from drawables
    val satelliteBitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.terrain_satellite_map)
        } catch (e: Exception) {
            null
        }
    }
    val heightmapBitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.terrain_heightmap)
        } catch (e: Exception) {
            null
        }
    }

    // Strobe light animation state
    var strobeTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            strobeTick++
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("flight_3d_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onZoom(zoom)
                    }
                    if (pan.x != 0f || pan.y != 0f) {
                        onOrbit(-pan.x * 0.35f, pan.y * 0.35f)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw Cockpit Dark Background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0C1427), Color(0xFF030712)),
                center = Offset(width * 0.5f, height * 0.45f),
                radius = max(width, height) * 0.85f
            )
        )

        // Initialize 3D Projector
        val eyePos = cameraState.computeEyePosition(flightState)
        val targetPos = cameraState.computeLookAtTarget(flightState)
        val projector = PerspectiveProjector(eyePos, targetPos, width, height, fovDegrees = 50.0)

        // 2. Draw Realistic 3D Textured Terrain Plane (Satellite Imagery / Grayscale Heightmap)
        val activeBitmap = when (terrainMode) {
            TerrainTextureMode.SATELLITE_COLOR -> satelliteBitmap
            TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> heightmapBitmap
            TerrainTextureMode.TACTICAL_GRID -> null
        }

        if (activeBitmap != null) {
            drawIntoCanvas { canvas ->
                drawTexturedTerrainMesh(
                    nativeCanvas = canvas.nativeCanvas,
                    projector = projector,
                    texture = activeBitmap,
                    terrainMode = terrainMode
                )
            }
        } else {
            // Minimalist tactical grid fallback
            drawGroundGrid(projector)
        }

        // 3. Draw Terrain Region Context Labels (Mt. Takamagahara, Sagami Bay, Mt. Fuji, etc.)
        drawTerrainSpatialAnnotations(projector, terrainMode)

        // 4. Draw Geographic Landmarks
        drawLandmarks(projector, terrainMode)

        // 5. Draw Trajectory Ground Shadow & Vertical Curtain
        drawAltitudeCurtain(projector)

        // 6. Draw 3D Trajectory Ribbon (Color-graded Catmull-Rom spline)
        drawTrajectoryPath(projector)

        // 7. Draw Waypoint Pins & Milestones
        drawMilestoneWaypoints(projector, flightState.waypointIndex)

        // 8. Draw Procedural Boeing 747 Model
        drawBoeing747(projector, flightState, isStrobeOn = strobeTick % 2 == 0L)
    }
}

/**
 * Render hardware-accelerated 3D textured terrain mesh across the Central Japan ground plane
 * covering Sagami Bay, Tokyo Bay, Mount Fuji, and Mount Takamagahara.
 */
private fun drawTexturedTerrainMesh(
    nativeCanvas: AndroidCanvas,
    projector: PerspectiveProjector,
    texture: Bitmap,
    terrainMode: TerrainTextureMode
) {
    val subdivisions = 24 // 24x24 cells = 576 quads = 1,152 triangles
    val vertexCountPerAxis = subdivisions + 1
    val totalVertices = vertexCountPerAxis * vertexCountPerAxis

    val bounds = TrajectoryMath.TERRAIN_BOUNDS
    val stepSize = (bounds * 2.0) / subdivisions

    val bmpW = texture.width.toFloat()
    val bmpH = texture.height.toFloat()

    // Allocate arrays for drawVertices
    val verts = FloatArray(totalVertices * 2)
    val texCoords = FloatArray(totalVertices * 2)
    val isVertexValid = BooleanArray(totalVertices)

    // 1. Compute and project all 3D mesh vertices
    var vIdx = 0
    for (j in 0..subdivisions) {
        val z = -bounds + j * stepSize
        val v = (j.toFloat() / subdivisions).coerceIn(0f, 1f)

        for (i in 0..subdivisions) {
            val x = -bounds + i * stepSize
            val u = (i.toFloat() / subdivisions).coerceIn(0f, 1f)

            // Calculate elevation for realistic 3D mountain relief
            val y = TrajectoryMath.getTerrainElevation(x, z)
            val worldPt = Vector3D(x, y, z)
            val projected = projector.project(worldPt)

            val vBase = vIdx * 2
            verts[vBase] = projected.offset.x
            verts[vBase + 1] = projected.offset.y

            texCoords[vBase] = u * bmpW
            texCoords[vBase + 1] = v * bmpH

            // Vertex is valid if it's in front of camera
            isVertexValid[vIdx] = projected.depth > 1.0
            vIdx++
        }
    }

    // 2. Build triangle index buffer
    val maxIndices = subdivisions * subdivisions * 6
    val indices = ShortArray(maxIndices)
    var indexCount = 0

    for (j in 0 until subdivisions) {
        for (i in 0 until subdivisions) {
            val i00 = j * vertexCountPerAxis + i
            val i10 = i00 + 1
            val i01 = (j + 1) * vertexCountPerAxis + i
            val i11 = i01 + 1

            // Triangle 1: i00, i10, i11
            if (isVertexValid[i00] && isVertexValid[i10] && isVertexValid[i11]) {
                indices[indexCount++] = i00.toShort()
                indices[indexCount++] = i10.toShort()
                indices[indexCount++] = i11.toShort()
            }

            // Triangle 2: i00, i11, i01
            if (isVertexValid[i00] && isVertexValid[i11] && isVertexValid[i01]) {
                indices[indexCount++] = i00.toShort()
                indices[indexCount++] = i11.toShort()
                indices[indexCount++] = i01.toShort()
            }
        }
    }

    if (indexCount > 0) {
        val shader = BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = AndroidPaint().apply {
            this.shader = shader
            isAntiAlias = true
            isFilterBitmap = true
            alpha = if (terrainMode == TerrainTextureMode.GRAYSCALE_HEIGHTMAP) 235 else 250
        }

        nativeCanvas.drawVertices(
            AndroidCanvas.VertexMode.TRIANGLES,
            totalVertices * 2,
            verts,
            0,
            texCoords,
            0,
            null,
            0,
            indices,
            0,
            indexCount,
            paint
        )
    }
}

/**
 * Draw spatial labels directly on the terrain surface to orient the viewer with key geography.
 */
private fun DrawScope.drawTerrainSpatialAnnotations(
    projector: PerspectiveProjector,
    terrainMode: TerrainTextureMode
) {
    val annotations = listOf(
        Triple("SAGAMI BAY", Vector3D(55.0, 0.2, 66.0), Color(0xFF38BDF8)),
        Triple("SURUGA BAY", Vector3D(-99.0, 0.2, 110.0), Color(0xFF38BDF8)),
        Triple("TOKYO BAY / HANEDA", Vector3D(150.0, 0.5, -33.0), Color(0xFF38BDF8)),
        Triple("MT. FUJI (3,776m)", Vector3D(-82.0, 8.5, 8.8), Color(0xFFF8FAFC)),
        Triple("MT. TAKAMAGAHARA (CRASH SITE)", Vector3D(-89.5, 4.5, -131.5), Color(0xFFA855F7)),
        Triple("CHICHIBU MOUNTAINS", Vector3D(-15.0, 3.2, -88.0), Color(0xFF94A3B8)),
        Triple("OTSUKI", Vector3D(-26.0, 1.2, -44.0), Color(0xFFF59E0B))
    )

    val labelPaint = AndroidPaint().apply {
        isAntiAlias = true
        textSize = 24f
        typeface = android.graphics.Typeface.MONOSPACE
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }

    drawIntoCanvas { canvas ->
        annotations.forEach { (text, pos, color) ->
            val pr = projector.project(pos)
            if (pr.isVisible) {
                labelPaint.color = color.toArgb()
                canvas.nativeCanvas.drawText(
                    text,
                    pr.offset.x - (text.length * 6.5f),
                    pr.offset.y,
                    labelPaint
                )
            }
        }
    }
}

/**
 * Draw tactical reference grid and compass axes on the ground plane (Y = 0).
 */
private fun DrawScope.drawGroundGrid(projector: PerspectiveProjector) {
    val gridColor = Color(0x1A38BDF8)
    val axisColor = Color(0x3338BDF8)

    val step = 30.0
    val range = 150.0

    var v = -range
    while (v <= range) {
        val p1 = projector.project(Vector3D(v, 0.0, -range))
        val p2 = projector.project(Vector3D(v, 0.0, range))
        if (p1.isVisible || p2.isVisible) {
            drawLine(
                color = if (abs(v) < 1.0) axisColor else gridColor,
                start = p1.offset,
                end = p2.offset,
                strokeWidth = if (abs(v) < 1.0) 1.5f else 0.8f
            )
        }

        val q1 = projector.project(Vector3D(-range, 0.0, v))
        val q2 = projector.project(Vector3D(range, 0.0, v))
        if (q1.isVisible || q2.isVisible) {
            drawLine(
                color = if (abs(v) < 1.0) axisColor else gridColor,
                start = q1.offset,
                end = q2.offset,
                strokeWidth = if (abs(v) < 1.0) 1.5f else 0.8f
            )
        }
        v += step
    }

    listOf(40.0, 90.0, 140.0).forEach { r ->
        val ringPts = (0..36).map { i ->
            val rad = i * 2 * PI / 36
            projector.project(Vector3D(cos(rad) * r, 0.0, sin(rad) * r))
        }
        for (i in 0 until ringPts.size - 1) {
            val a = ringPts[i]
            val b = ringPts[i + 1]
            if (a.isVisible || b.isVisible) {
                drawLine(
                    color = Color(0x1438BDF8),
                    start = a.offset,
                    end = b.offset,
                    strokeWidth = 1f
                )
            }
        }
    }
}

/**
 * Draw mountain peaks and regional landmarks.
 */
private fun DrawScope.drawLandmarks(
    projector: PerspectiveProjector,
    terrainMode: TerrainTextureMode
) {
    FlightData.LANDMARKS.forEach { lm ->
        val groundPos = TrajectoryMath.geoTo3D(lm.lon, lm.lat, 0.0)
        val peakPos = TrajectoryMath.geoTo3D(lm.lon, lm.lat, lm.elevationM * 3.28084)

        val prGround = projector.project(groundPos)
        val prPeak = projector.project(peakPos)

        if (prGround.isVisible || prPeak.isVisible) {
            when (lm.type) {
                LandmarkType.MOUNTAIN -> {
                    // Accent wireframe for Mount Fuji peak
                    val baseRadius = if (lm.name.contains("Fuji")) 14.0 else 8.0
                    val segments = 8
                    val basePoints = (0 until segments).map { idx ->
                        val rad = idx * 2 * PI / segments
                        projector.project(groundPos + Vector3D(cos(rad) * baseRadius, 0.0, sin(rad) * baseRadius))
                    }
                    basePoints.forEach { bp ->
                        drawLine(
                            color = Color(0x44FFFFFF),
                            start = bp.offset,
                            end = prPeak.offset,
                            strokeWidth = 1.0f
                        )
                    }
                    if (lm.name.contains("Fuji")) {
                        drawCircle(color = Color(0xEEFFFFFF), radius = 4f, center = prPeak.offset)
                    }
                }
                LandmarkType.CRASH_SITE -> {
                    // Pulsing beacon on Mt. Takamagahara ridge
                    drawLine(
                        color = HudPurple,
                        start = prGround.offset,
                        end = prPeak.offset,
                        strokeWidth = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                    drawCircle(color = HudPurple.copy(alpha = 0.35f), radius = 14f, center = prPeak.offset)
                    drawCircle(color = Color.White, radius = 4f, center = prPeak.offset)
                }
                LandmarkType.VOR_NAV -> {
                    drawLine(
                        color = HudAmber.copy(alpha = 0.8f),
                        start = prGround.offset,
                        end = prGround.offset + Offset(0f, -14f),
                        strokeWidth = 1.8f
                    )
                    drawCircle(color = HudAmber, radius = 3.5f, center = prGround.offset + Offset(0f, -14f))
                }
                LandmarkType.BAY -> {
                    if (terrainMode == TerrainTextureMode.TACTICAL_GRID) {
                        drawCircle(color = Color(0x1A0284C7), radius = 28f, center = prGround.offset)
                    }
                }
                LandmarkType.AIRPORT, LandmarkType.MILITARY_BASE -> {
                    drawLine(
                        color = HudCyan.copy(alpha = 0.8f),
                        start = prGround.offset + Offset(-10f, 6f),
                        end = prGround.offset + Offset(10f, -6f),
                        strokeWidth = 3f
                    )
                    drawCircle(color = HudCyan, radius = 3f, center = prGround.offset)
                }
            }
        }
    }
}

/**
 * Draw semi-transparent vertical curtain connecting the flight trajectory to the ground.
 */
private fun DrawScope.drawAltitudeCurtain(projector: PerspectiveProjector) {
    val samplePoints = TrajectoryMath.PRECOMPUTED_POINTS
    val step = 4

    var i = 0
    while (i < samplePoints.size - step) {
        val p1 = samplePoints[i]
        val p2 = samplePoints[i + step]

        val g1 = Vector3D(p1.x, TrajectoryMath.getTerrainElevation(p1.x, p1.z), p1.z)
        val g2 = Vector3D(p2.x, TrajectoryMath.getTerrainElevation(p2.x, p2.z), p2.z)

        val pr1 = projector.project(p1)
        val pr2 = projector.project(p2)
        val gr1 = projector.project(g1)
        val gr2 = projector.project(g2)

        if (pr1.isVisible || pr2.isVisible || gr1.isVisible || gr2.isVisible) {
            val altFt = p1.y / TrajectoryMath.ALTITUDE_SCALE
            val altColor = getAltitudeColor(altFt)

            // Vertical drop line
            drawLine(
                color = altColor.copy(alpha = 0.15f),
                start = gr1.offset,
                end = pr1.offset,
                strokeWidth = 1f
            )

            // Ground shadow segment
            drawLine(
                color = Color(0x440284C7),
                start = gr1.offset,
                end = gr2.offset,
                strokeWidth = 1.4f
            )

            // Curtain polygon fill
            val path = Path().apply {
                moveTo(pr1.offset.x, pr1.offset.y)
                lineTo(pr2.offset.x, pr2.offset.y)
                lineTo(gr2.offset.x, gr2.offset.y)
                lineTo(gr1.offset.x, gr1.offset.y)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(altColor.copy(alpha = 0.22f), altColor.copy(alpha = 0.02f)),
                    startY = min(pr1.offset.y, pr2.offset.y),
                    endY = max(gr1.offset.y, gr2.offset.y)
                )
            )
        }
        i += step
    }
}

/**
 * Draw high-resolution color-graded 3D trajectory spline ribbon.
 */
private fun DrawScope.drawTrajectoryPath(projector: PerspectiveProjector) {
    val samplePoints = TrajectoryMath.PRECOMPUTED_POINTS

    for (i in 0 until samplePoints.size - 1) {
        val p1 = samplePoints[i]
        val p2 = samplePoints[i + 1]

        val pr1 = projector.project(p1)
        val pr2 = projector.project(p2)

        if (pr1.isVisible && pr2.isVisible) {
            val altFt = ((p1.y + p2.y) * 0.5) / TrajectoryMath.ALTITUDE_SCALE
            val col = getAltitudeColor(altFt)

            // Outer glow line
            drawLine(
                color = col.copy(alpha = 0.35f),
                start = pr1.offset,
                end = pr2.offset,
                strokeWidth = 5.5f,
                cap = StrokeCap.Round
            )
            // Core crisp ribbon line
            drawLine(
                color = col,
                start = pr1.offset,
                end = pr2.offset,
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Draw landmark waypoint markers with elevation posts and milestone markers.
 */
private fun DrawScope.drawMilestoneWaypoints(
    projector: PerspectiveProjector,
    activeWaypointIndex: Int
) {
    val criticalIndices = listOf(0, 4, 7, 11, 17, 23)

    criticalIndices.forEach { idx ->
        if (idx in FlightData.WAYPOINTS.indices) {
            val wp = FlightData.WAYPOINTS[idx]
            val pos = TrajectoryMath.geoTo3D(wp.lon, wp.lat, wp.altFt)
            val groundY = TrajectoryMath.getTerrainElevation(pos.x, pos.z)
            val groundPos = Vector3D(pos.x, groundY, pos.z)

            val pr = projector.project(pos)
            val gr = projector.project(groundPos)

            if (pr.isVisible) {
                val isSelected = (idx == activeWaypointIndex)

                // Vertical dashed altitude stem to ground
                drawLine(
                    color = Color(0x6694A3B8),
                    start = gr.offset,
                    end = pr.offset,
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )

                val dotColor = when (idx) {
                    0 -> HudCyan
                    4 -> HudRedJal // Bulkhead rupture
                    11 -> HudAmber // Otsuki orbit
                    17 -> AltMidCyan // Lowest altitude dive
                    23 -> HudPurple // Crash ridge
                    else -> HudEmerald
                }

                if (isSelected) {
                    drawCircle(color = dotColor.copy(alpha = 0.4f), radius = 12f, center = pr.offset)
                }
                drawCircle(color = dotColor, radius = if (isSelected) 5.5f else 4f, center = pr.offset)
                drawCircle(color = Color.White, radius = 1.8f, center = pr.offset)
            }
        }
    }
}

/**
 * Draw the procedural 3D Boeing 747 model with dynamic bank, pitch, yaw, and tail status.
 */
private fun DrawScope.drawBoeing747(
    projector: PerspectiveProjector,
    state: InterpolatedFlightState,
    isStrobeOn: Boolean
) {
    val segments = Boeing747Model.getTransformedSegments(state, scale = Boeing747Model.MODEL_SCALE)

    segments.forEach { (w1, w2) ->
        val pr1 = projector.project(w1)
        val pr2 = projector.project(w2)

        if (pr1.isVisible && pr2.isVisible) {
            val isTailSegment = (w1.y > state.position3D.y + 0.8 || w2.y > state.position3D.y + 0.8) &&
                    (w1.z < state.position3D.z - 2.0 || w2.z < state.position3D.z - 2.0)

            val lineColor = if (isTailSegment) {
                if (state.isDecompressed) Color(0xFF7F1D1D) else HudRedJal
            } else {
                Color(0xFFF1F5F9)
            }

            val stroke = if (isTailSegment) 2.2f else 1.6f

            drawLine(
                color = lineColor,
                start = pr1.offset,
                end = pr2.offset,
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    val planeCenterPr = projector.project(state.position3D)
    if (planeCenterPr.isVisible) {
        val haloColor = if (state.isDecompressed) HudRed.copy(alpha = 0.25f) else HudCyan.copy(alpha = 0.2f)
        drawCircle(color = haloColor, radius = 16f, center = planeCenterPr.offset)
    }

    val (beaconPos, greenNavPos, redNavPos) = Boeing747Model.getStrobeLightPositions(state, Boeing747Model.MODEL_SCALE)
    val prBeacon = projector.project(beaconPos)
    val prGreen = projector.project(greenNavPos)
    val prRed = projector.project(redNavPos)

    if (prBeacon.isVisible && isStrobeOn) {
        drawCircle(color = Color(0xFFFF2222), radius = 3.5f, center = prBeacon.offset)
        drawCircle(color = Color(0x66FF0000), radius = 8f, center = prBeacon.offset)
    }

    if (prGreen.isVisible) {
        drawCircle(color = Color(0xFF22C55E), radius = 2.5f, center = prGreen.offset)
    }
    if (prRed.isVisible) {
        drawCircle(color = Color(0xFFEF4444), radius = 2.5f, center = prRed.offset)
    }
}

/**
 * Map altitude in feet to gradient color spectrum.
 */
fun getAltitudeColor(altFt: Double): Color {
    return when {
        altFt <= 7000.0 -> AltLowGreen
        altFt <= 14000.0 -> AltMidCyan
        altFt <= 21000.0 -> AltMidYellow
        altFt <= 23800.0 -> AltHighOrange
        else -> AltMaxRed
    }
}
