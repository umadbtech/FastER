package com.faster.festival.ui.pinch.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════════════
// Map colors matching the wireframe
// ═══════════════════════════════════════════════════════════════════════════════

private val MapGreenLight = Color(0xFFD4E8C4)
private val MapGreenMid = Color(0xFFC2DEB0)
private val MapGreenDark = Color(0xFFB5D6A0)
private val MapGreenPatch = Color(0xFFBED8AD)
private val RoadColor = Color(0xFFE8E4DA)
private val RoadOutline = Color(0xFFD4D0C6)
private val WaterBlue = Color(0xFFB3D4E8)
private val WaterBlueDark = Color(0xFF9BC4DC)
private val RouteBlue = Color(0xFF1A237E)
private val PinNavy = Color(0xFF2C3E6B)
private val MarkerRed = Color(0xFFD32F2F)
private val ResponderRed = Color(0xFFE53935)

enum class FakeMapState {
    ALERT_SENT,
    ANSWER_CALL,
    HELP_ON_THE_WAY,
    HELP_ARRIVED,
    HELP_IN_PROGRESS,
    RESOLVED,
    DEFAULT
}

/**
 * Drop-in replacement for LiveMapBackground.
 * Renders a polished fake satellite/terrain map entirely in Compose Canvas
 * with medical markers, user pin, route line, and responder arrow.
 */
@Composable
fun FakeEmergencyMapBackground(
    mapState: FakeMapState = FakeMapState.DEFAULT,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Canvas-drawn map terrain
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMapTerrain()
            drawWaterFeature()
            drawRoads()

            if (mapState == FakeMapState.HELP_ON_THE_WAY ||
                mapState == FakeMapState.HELP_ARRIVED
            ) {
                drawRouteLine(mapState)
            }
        }

        // Overlay markers using Compose (so they look crisp)
        MedicalMarkers()
        UserPin(mapState)

        if (mapState == FakeMapState.HELP_ON_THE_WAY ||
            mapState == FakeMapState.HELP_ARRIVED
        ) {
            ResponderArrow(mapState)
        }

        // Screen content on top
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Canvas: Terrain
// ═══════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawMapTerrain() {
    // Base green
    drawRect(color = MapGreenLight)

    // Terrain patches for depth
    val w = size.width
    val h = size.height

    // Large darker patch top-left
    val patchPath1 = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.45f, 0f)
        lineTo(w * 0.35f, h * 0.3f)
        lineTo(w * 0.15f, h * 0.45f)
        lineTo(0f, h * 0.35f)
        close()
    }
    drawPath(patchPath1, color = MapGreenDark)

    // Mid patch center-right
    val patchPath2 = Path().apply {
        moveTo(w * 0.55f, h * 0.15f)
        lineTo(w, h * 0.1f)
        lineTo(w, h * 0.5f)
        lineTo(w * 0.65f, h * 0.55f)
        lineTo(w * 0.45f, h * 0.35f)
        close()
    }
    drawPath(patchPath2, color = MapGreenMid)

    // Small patch bottom
    val patchPath3 = Path().apply {
        moveTo(w * 0.1f, h * 0.6f)
        lineTo(w * 0.5f, h * 0.55f)
        lineTo(w * 0.55f, h * 0.75f)
        lineTo(w * 0.2f, h * 0.8f)
        close()
    }
    drawPath(patchPath3, color = MapGreenPatch)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Canvas: Water feature (diagonal river/stream)
// ═══════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawWaterFeature() {
    val w = size.width
    val h = size.height

    // Curved water body from top-right to mid-right
    val waterPath = Path().apply {
        moveTo(w * 0.72f, 0f)
        cubicTo(
            w * 0.68f, h * 0.12f,
            w * 0.78f, h * 0.25f,
            w * 0.7f, h * 0.4f
        )
        cubicTo(
            w * 0.62f, h * 0.55f,
            w * 0.75f, h * 0.65f,
            w * 0.85f, h * 0.7f
        )
        lineTo(w, h * 0.68f)
        lineTo(w, 0f)
        close()
    }

    drawPath(
        waterPath,
        brush = Brush.verticalGradient(
            colors = listOf(WaterBlue, WaterBlueDark, WaterBlue)
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Canvas: Roads
// ═══════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawRoads() {
    val w = size.width
    val h = size.height
    val roadWidth = 12f

    // Main diagonal road top-left to center-right
    val road1 = Path().apply {
        moveTo(0f, h * 0.22f)
        cubicTo(
            w * 0.2f, h * 0.25f,
            w * 0.35f, h * 0.32f,
            w * 0.55f, h * 0.38f
        )
        cubicTo(
            w * 0.65f, h * 0.42f,
            w * 0.7f, h * 0.48f,
            w * 0.68f, h * 0.55f
        )
    }
    drawPath(road1, color = RoadOutline, style = Stroke(width = roadWidth + 4f, cap = StrokeCap.Round))
    drawPath(road1, color = RoadColor, style = Stroke(width = roadWidth, cap = StrokeCap.Round))

    // Secondary road from top-center going down
    val road2 = Path().apply {
        moveTo(w * 0.42f, 0f)
        cubicTo(
            w * 0.44f, h * 0.15f,
            w * 0.38f, h * 0.35f,
            w * 0.4f, h * 0.55f
        )
        lineTo(w * 0.42f, h * 0.75f)
    }
    drawPath(road2, color = RoadOutline, style = Stroke(width = roadWidth + 3f, cap = StrokeCap.Round))
    drawPath(road2, color = RoadColor, style = Stroke(width = roadWidth, cap = StrokeCap.Round))

    // Small road from left
    val road3 = Path().apply {
        moveTo(0f, h * 0.52f)
        cubicTo(
            w * 0.15f, h * 0.48f,
            w * 0.25f, h * 0.44f,
            w * 0.38f, h * 0.42f
        )
    }
    drawPath(road3, color = RoadOutline, style = Stroke(width = roadWidth + 2f, cap = StrokeCap.Round))
    drawPath(road3, color = RoadColor, style = Stroke(width = roadWidth - 2f, cap = StrokeCap.Round))

    // Road going bottom-right
    val road4 = Path().apply {
        moveTo(w * 0.55f, h * 0.38f)
        cubicTo(
            w * 0.62f, h * 0.5f,
            w * 0.58f, h * 0.65f,
            w * 0.65f, h * 0.8f
        )
        lineTo(w * 0.7f, h)
    }
    drawPath(road4, color = RoadOutline, style = Stroke(width = roadWidth + 2f, cap = StrokeCap.Round))
    drawPath(road4, color = RoadColor, style = Stroke(width = roadWidth - 2f, cap = StrokeCap.Round))
}

// ═══════════════════════════════════════════════════════════════════════════════
// Canvas: Route line (responder → user)
// ═══════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawRouteLine(state: FakeMapState) {
    val w = size.width
    val h = size.height

    // User pin position: center-left area
    val userX = w * 0.38f
    val userY = h * 0.40f

    // Responder position varies by state
    val respX: Float
    val respY: Float
    when (state) {
        FakeMapState.HELP_ON_THE_WAY -> {
            respX = w * 0.6f
            respY = h * 0.52f
        }
        FakeMapState.HELP_ARRIVED -> {
            respX = w * 0.42f
            respY = h * 0.43f
        }
        else -> return
    }

    val routePath = Path().apply {
        moveTo(userX, userY)
        cubicTo(
            userX + (respX - userX) * 0.3f, userY + (respY - userY) * 0.1f,
            userX + (respX - userX) * 0.7f, userY + (respY - userY) * 0.9f,
            respX, respY
        )
    }

    drawPath(
        routePath,
        color = RouteBlue,
        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Compose overlays: Medical markers
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MedicalMarkers() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-center medical tent
        MedicalTentMarker(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, end = 30.dp)
        )

        // Left medical tent
        MedicalTentMarker(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp, bottom = 80.dp)
        )

        // Bottom-right medical tent
        MedicalTentMarker(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 40.dp, top = 100.dp)
        )
    }
}

@Composable
private fun MedicalTentMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MarkerRed),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalHospital,
            contentDescription = "Medical Station",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Compose overlay: User location pin
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun UserPin(mapState: FakeMapState) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Pin position: center-ish, slightly left
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-30).dp, y = (-30).dp)
                .size(24.dp, 32.dp)
        ) {
            val cx = size.width / 2f
            val pinRadius = size.width / 2f

            // Pin body (teardrop)
            val pinPath = Path().apply {
                moveTo(cx, size.height)
                cubicTo(
                    cx - pinRadius * 0.3f, size.height * 0.65f,
                    cx - pinRadius, size.height * 0.45f,
                    cx - pinRadius, size.height * 0.35f
                )
                cubicTo(
                    cx - pinRadius, size.height * 0.1f,
                    cx - pinRadius * 0.3f, 0f,
                    cx, 0f
                )
                cubicTo(
                    cx + pinRadius * 0.3f, 0f,
                    cx + pinRadius, size.height * 0.1f,
                    cx + pinRadius, size.height * 0.35f
                )
                cubicTo(
                    cx + pinRadius, size.height * 0.45f,
                    cx + pinRadius * 0.3f, size.height * 0.65f,
                    cx, size.height
                )
                close()
            }

            drawPath(pinPath, color = PinNavy)

            // White dot inside
            drawCircle(
                color = Color.White,
                radius = pinRadius * 0.35f,
                center = Offset(cx, size.height * 0.33f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Compose overlay: Responder direction arrow
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResponderArrow(mapState: FakeMapState) {
    val offsetX: Int
    val offsetY: Int
    val rotation: Float

    when (mapState) {
        FakeMapState.HELP_ON_THE_WAY -> {
            offsetX = 50
            offsetY = 40
            rotation = -45f
        }
        FakeMapState.HELP_ARRIVED -> {
            offsetX = 10
            offsetY = 5
            rotation = -45f
        }
        else -> return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Red directional marker
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = offsetX.dp, y = offsetY.dp)
                .rotate(rotation)
                .size(20.dp, 24.dp)
        ) {
            val w = size.width
            val h = size.height

            // Triangle arrow pointing up
            val arrowPath = Path().apply {
                moveTo(w / 2f, 0f)
                lineTo(w, h)
                lineTo(w / 2f, h * 0.7f)
                lineTo(0f, h)
                close()
            }
            drawPath(arrowPath, color = ResponderRed)
        }
    }
}
