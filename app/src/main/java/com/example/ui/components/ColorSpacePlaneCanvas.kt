package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.colorengine.*
import kotlin.math.*

enum class ColorPlaneAxis {
    A_VS_B,   // Chromaticity (a* Red/Green vs b* Yellow/Blue)
    L_VS_B    // Lightness (L*) vs Yellowness (b*)
}

/**
 * Custom Canvas-based 2D Color Space component plotting scanned tooth L*a*b*
 * against the 16 VITA Classical targets with clinical tolerance zones.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun ColorSpacePlaneCanvas(
    scannedLab: Lab,
    targetShade: String,
    modifier: Modifier = Modifier,
    onSelectShade: ((String) -> Unit)? = null
) {
    var selectedAxis by remember { mutableStateOf(ColorPlaneAxis.A_VS_B) }
    var inspectShadeCode by remember(targetShade) { mutableStateOf(targetShade) }
    val textMeasurer = rememberTextMeasurer()

    val targetRef = remember(inspectShadeCode) {
        VitaClassicalData.getByCode(inspectShadeCode)
            ?: VitaClassicalData.getByCode(targetShade)
            ?: VitaClassicalData.ALL_16_SHADES.first()
    }

    val deltaE00 = remember(scannedLab, targetRef.lab) {
        Ciede2000.calculate(scannedLab, targetRef.lab)
    }

    // Gentle pulse animation for the scanned tooth marker
    val infiniteTransition = rememberInfiniteTransition(label = "marker_pulse")
    val pulseRadiusAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("color_space_plane_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title and Axis Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2D Color Space Confirmation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CIELAB plane: Scanned tooth vs. VITA reference targets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Plane Switcher (a*-b* chromaticity or L*-b* lightness)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    PlaneToggleButton(
                        text = "a* - b*",
                        isSelected = selectedAxis == ColorPlaneAxis.A_VS_B,
                        onClick = { selectedAxis = ColorPlaneAxis.A_VS_B }
                    )
                    PlaneToggleButton(
                        text = "L* - b*",
                        isSelected = selectedAxis == ColorPlaneAxis.L_VS_B,
                        onClick = { selectedAxis = ColorPlaneAxis.L_VS_B }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Diagnostic Status Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7))
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Text(
                            text = "Tooth (L*%.1f, a*%.1f, b*%.1f)".format(scannedLab.l, scannedLab.a, scannedLab.b),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (deltaE00 <= 1.8) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Target ${targetRef.shadeCode} • ΔE00 %.2f".format(deltaE00),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (deltaE00 <= 1.8) Color(0xFF047857) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main 2D Canvas Plot & L* Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // The 2D Canvas Plot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)) // Dark slate laboratory oscilloscope background
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                ) {
                    val density = LocalDensity.current

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("color_space_canvas")
                            .pointerInput(selectedAxis) {
                                detectTapGestures { tapOffset ->
                                    // Hit test 16 VITA shades to allow tapping to inspect
                                    val tappedShade = findClosestVitaShadeAtTap(
                                        tapOffset = tapOffset,
                                        canvasWidth = size.width.toFloat(),
                                        canvasHeight = size.height.toFloat(),
                                        axis = selectedAxis
                                    )
                                    if (tappedShade != null) {
                                        inspectShadeCode = tappedShade.shadeCode
                                        onSelectShade?.invoke(tappedShade.shadeCode)
                                    }
                                }
                            }
                    ) {
                        drawColorSpacePlane(
                            scannedLab = scannedLab,
                            targetRef = targetRef,
                            allShades = VitaClassicalData.ALL_16_SHADES,
                            axis = selectedAxis,
                            pulseRadius = pulseRadiusAnim * density.density,
                            pulseAlpha = pulseAlphaAnim,
                            textMeasurer = textMeasurer
                        )
                    }
                }

                // Vertical L* Lightness Gauge (when in a*-b* mode)
                if (selectedAxis == ColorPlaneAxis.A_VS_B) {
                    LightnessVerticalGauge(
                        scannedL = scannedLab.l,
                        targetL = targetRef.lab.l,
                        targetCode = targetRef.shadeCode,
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend and Explanation Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF38BDF8), label = "Scanned Tooth")
                    LegendItem(color = Color(0xFFF59E0B), label = "Closest Target (${targetRef.shadeCode})")
                    LegendItem(color = Color(0xFF94A3B8), label = "VITA Standards")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(1.dp, Color(0xFF10B981), CircleShape)
                    )
                    Text("ΔE≤1.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(1.dp, Color(0xFFEAB308), CircleShape)
                    )
                    Text("ΔE≤2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlaneToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Vertical Lightness (L*) Gauge
 */
@Composable
private fun LightnessVerticalGauge(
    scannedL: Double,
    targetL: Double,
    targetCode: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "L*",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )

            // Vertical gradient bar representing Lightness scale (100 White -> 0 Black)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFC), // White 100
                                Color(0xFFE2E8F0),
                                Color(0xFF94A3B8),
                                Color(0xFF475569),
                                Color(0xFF1E293B)  // Dark 0
                            )
                        )
                    )
            ) {
                // Marker for Target L* (Range 60 to 90 for dental tooth enamel)
                val minL = 60.0
                val maxL = 90.0
                val targetFrac = (1.0 - ((targetL.coerceIn(minL, maxL) - minL) / (maxL - minL))).toFloat()
                val toothFrac = (1.0 - ((scannedL.coerceIn(minL, maxL) - minL) / (maxL - minL))).toFloat()

                // Target indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(targetFrac)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(Color(0xFFF59E0B))
                    )
                }

                // Tooth indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(toothFrac)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(Color(0xFF38BDF8))
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(scannedL),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "vs %.1f".format(targetL),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = Color(0xFFF59E0B),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Custom Canvas drawing logic for the 2D CIELAB plane
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawColorSpacePlane(
    scannedLab: Lab,
    targetRef: VitaReference,
    allShades: List<VitaReference>,
    axis: ColorPlaneAxis,
    pulseRadius: Float,
    pulseAlpha: Float,
    textMeasurer: TextMeasurer
) {
    val pad = 36f
    val plotWidth = size.width - pad * 2
    val plotHeight = size.height - pad * 2

    // Coordinate ranges
    // For a*-b* plane: a* range is typically [-3, +7], b* range is [+8, +28]
    // For L*-b* plane: L* range is [60, 90], b* range is [+8, +28]
    val minX: Float
    val maxX: Float
    val minY: Float
    val maxY: Float
    val xLabel: String
    val yLabel: String

    when (axis) {
        ColorPlaneAxis.A_VS_B -> {
            minX = -3.0f
            maxX = 7.0f
            minY = 8.0f
            maxY = 28.0f
            xLabel = "a* (← Greenish | Reddish →)"
            yLabel = "b* (Yellowish ↑)"
        }
        ColorPlaneAxis.L_VS_B -> {
            minX = 64.0f
            maxX = 88.0f
            minY = 8.0f
            maxY = 28.0f
            xLabel = "L* (Lightness Scale)"
            yLabel = "b* (Chroma/Yellowness)"
        }
    }

    fun mapToScreen(xVal: Double, yVal: Double): Offset {
        val normX = ((xVal.toFloat() - minX) / (maxX - minX)).coerceIn(0f, 1f)
        val normY = ((yVal.toFloat() - minY) / (maxY - minY)).coerceIn(0f, 1f)
        val sx = pad + normX * plotWidth
        val sy = pad + (1f - normY) * plotHeight // Y inverted in screen coords
        return Offset(sx, sy)
    }

    // 1. Draw Grid Lines
    val gridColor = Color(0xFF1E293B)
    val axisColor = Color(0xFF475569)

    // Vertical grid
    val xSteps = 5
    for (i in 0..xSteps) {
        val gx = pad + (plotWidth / xSteps) * i
        drawLine(
            color = gridColor,
            start = Offset(gx, pad),
            end = Offset(gx, pad + plotHeight),
            strokeWidth = 1f
        )
        val valX = minX + (maxX - minX) * (i.toFloat() / xSteps)
        val textLayout = textMeasurer.measure(
            text = AnnotatedString("%.0f".format(valX)),
            style = TextStyle(color = Color(0xFF64748B), fontSize = 9.sp)
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(gx - textLayout.size.width / 2f, pad + plotHeight + 4f)
        )
    }

    // Horizontal grid
    val ySteps = 4
    for (j in 0..ySteps) {
        val gy = pad + (plotHeight / ySteps) * j
        drawLine(
            color = gridColor,
            start = Offset(pad, gy),
            end = Offset(pad + plotWidth, gy),
            strokeWidth = 1f
        )
        val valY = maxY - (maxY - minY) * (j.toFloat() / ySteps)
        val textLayout = textMeasurer.measure(
            text = AnnotatedString("%.0f".format(valY)),
            style = TextStyle(color = Color(0xFF64748B), fontSize = 9.sp)
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(pad - textLayout.size.width - 6f, gy - textLayout.size.height / 2f)
        )
    }

    // Border of plot area
    drawRect(
        color = axisColor,
        topLeft = Offset(pad, pad),
        size = Size(plotWidth, plotHeight),
        style = Stroke(width = 1.5f)
    )

    // Axis Labels
    val xLabelLayout = textMeasurer.measure(
        text = AnnotatedString(xLabel),
        style = TextStyle(color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    )
    drawText(
        textLayoutResult = xLabelLayout,
        topLeft = Offset(pad + (plotWidth - xLabelLayout.size.width) / 2f, size.height - 18f)
    )

    val yLabelLayout = textMeasurer.measure(
        text = AnnotatedString(yLabel),
        style = TextStyle(color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    )
    drawText(
        textLayoutResult = yLabelLayout,
        topLeft = Offset(pad + 6f, pad - 20f)
    )

    // 2. Map coordinates of targets & scanned tooth
    val scannedCoord = when (axis) {
        ColorPlaneAxis.A_VS_B -> mapToScreen(scannedLab.a, scannedLab.b)
        ColorPlaneAxis.L_VS_B -> mapToScreen(scannedLab.l, scannedLab.b)
    }
    val targetCoord = when (axis) {
        ColorPlaneAxis.A_VS_B -> mapToScreen(targetRef.lab.a, targetRef.lab.b)
        ColorPlaneAxis.L_VS_B -> mapToScreen(targetRef.lab.l, targetRef.lab.b)
    }

    // 3. Draw Tolerance Boundary Rings around Target (ΔE = 1.0 and ΔE = 2.0)
    // Scale 1 ΔE unit to pixels based on plot dimensions
    val pixelsPerUnitX = plotWidth / (maxX - minX)
    val pixelsPerUnitY = plotHeight / (maxY - minY)
    val avgPixelsPerUnit = (pixelsPerUnitX + pixelsPerUnitY) / 2f

    val radiusDe1 = 1.0f * avgPixelsPerUnit * 1.3f
    val radiusDe2 = 2.0f * avgPixelsPerUnit * 1.3f

    // Acceptance zone ΔE = 2.0
    drawCircle(
        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
        radius = radiusDe2,
        center = targetCoord
    )
    drawCircle(
        color = Color(0xFFF59E0B).copy(alpha = 0.5f),
        radius = radiusDe2,
        center = targetCoord,
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    )

    // Perfect match zone ΔE = 1.0
    drawCircle(
        color = Color(0xFF10B981).copy(alpha = 0.18f),
        radius = radiusDe1,
        center = targetCoord
    )
    drawCircle(
        color = Color(0xFF10B981).copy(alpha = 0.7f),
        radius = radiusDe1,
        center = targetCoord,
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
    )

    // 4. Connecting Vector Line from Scanned to Target
    drawLine(
        color = Color(0xFF38BDF8),
        start = scannedCoord,
        end = targetCoord,
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
    )

    // Midpoint distance tag
    val mid = Offset((scannedCoord.x + targetCoord.x) / 2f, (scannedCoord.y + targetCoord.y) / 2f)
    val de00 = Ciede2000.calculate(scannedLab, targetRef.lab)
    val distTagLayout = textMeasurer.measure(
        text = AnnotatedString("ΔE00 %.2f".format(de00)),
        style = TextStyle(color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    )
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(mid.x - distTagLayout.size.width / 2f - 4f, mid.y - distTagLayout.size.height / 2f - 2f),
        size = Size(distTagLayout.size.width + 8f, distTagLayout.size.height + 4f)
    )
    drawText(
        textLayoutResult = distTagLayout,
        topLeft = Offset(mid.x - distTagLayout.size.width / 2f, mid.y - distTagLayout.size.height / 2f)
    )

    // 5. Draw all 16 VITA Classical Reference Points
    allShades.forEach { ref ->
        val pos = when (axis) {
            ColorPlaneAxis.A_VS_B -> mapToScreen(ref.lab.a, ref.lab.b)
            ColorPlaneAxis.L_VS_B -> mapToScreen(ref.lab.l, ref.lab.b)
        }
        val isTarget = ref.shadeCode == targetRef.shadeCode
        val rgb = ColorConversions.labToRgb(ref.lab)
        val shadeColor = Color(rgb.first, rgb.second, rgb.third)

        if (isTarget) {
            // Draw highlight ring around target
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 12f,
                center = pos,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = shadeColor,
                radius = 8f,
                center = pos
            )
        } else {
            // Normal VITA shade dot
            drawCircle(
                color = Color(0xFF475569),
                radius = 7f,
                center = pos
            )
            drawCircle(
                color = shadeColor,
                radius = 5.5f,
                center = pos
            )
        }

        // Draw Shade Code Label
        val shadeLabelLayout = textMeasurer.measure(
            text = AnnotatedString(ref.shadeCode),
            style = TextStyle(
                color = if (isTarget) Color(0xFFFDE68A) else Color(0xFFCBD5E1),
                fontSize = if (isTarget) 10.sp else 8.5.sp,
                fontWeight = if (isTarget) FontWeight.ExtraBold else FontWeight.Medium
            )
        )
        val labelOffset = Offset(pos.x + 8f, pos.y - shadeLabelLayout.size.height / 2f)
        drawText(textLayoutResult = shadeLabelLayout, topLeft = labelOffset)
    }

    // 6. Draw Scanned Tooth Marker with Pulse Effect
    // Animated pulse circle
    if (pulseAlpha > 0.01f) {
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = pulseAlpha),
            radius = 12f + pulseRadius,
            center = scannedCoord,
            style = Stroke(width = 2f)
        )
    }

    // Reticle Crosshairs
    val crosshairLen = 14f
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(scannedCoord.x - crosshairLen, scannedCoord.y),
        end = Offset(scannedCoord.x + crosshairLen, scannedCoord.y),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(scannedCoord.x, scannedCoord.y - crosshairLen),
        end = Offset(scannedCoord.x, scannedCoord.y + crosshairLen),
        strokeWidth = 2f
    )

    // Center Core Tooth Dot
    val toothRgb = ColorConversions.labToRgb(scannedLab)
    val toothColor = Color(toothRgb.first, toothRgb.second, toothRgb.third)
    drawCircle(
        color = Color(0xFF38BDF8),
        radius = 8f,
        center = scannedCoord,
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = toothColor,
        radius = 5.5f,
        center = scannedCoord
    )

    // Tooth Label Banner
    val toothLabelLayout = textMeasurer.measure(
        text = AnnotatedString("Tooth"),
        style = TextStyle(color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    )
    drawText(
        textLayoutResult = toothLabelLayout,
        topLeft = Offset(scannedCoord.x - toothLabelLayout.size.width / 2f, scannedCoord.y - 24f)
    )
}

/**
 * Finds the closest VITA shade to a tap offset for interactivity
 */
private fun findClosestVitaShadeAtTap(
    tapOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    axis: ColorPlaneAxis
): VitaReference? {
    val pad = 36f
    val plotWidth = canvasWidth - pad * 2
    val plotHeight = canvasHeight - pad * 2

    val minX: Float
    val maxX: Float
    val minY = 8.0f
    val maxY = 28.0f

    when (axis) {
        ColorPlaneAxis.A_VS_B -> {
            minX = -3.0f
            maxX = 7.0f
        }
        ColorPlaneAxis.L_VS_B -> {
            minX = 64.0f
            maxX = 88.0f
        }
    }

    var bestMatch: VitaReference? = null
    var minDistance = Float.MAX_VALUE
    val hitRadiusPx = 28f

    VitaClassicalData.ALL_16_SHADES.forEach { ref ->
        val xVal = if (axis == ColorPlaneAxis.A_VS_B) ref.lab.a else ref.lab.l
        val yVal = ref.lab.b

        val normX = ((xVal.toFloat() - minX) / (maxX - minX)).coerceIn(0f, 1f)
        val normY = ((yVal.toFloat() - minY) / (maxY - minY)).coerceIn(0f, 1f)
        val sx = pad + normX * plotWidth
        val sy = pad + (1f - normY) * plotHeight

        val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
        if (dist < hitRadiusPx && dist < minDistance) {
            minDistance = dist
            bestMatch = ref
        }
    }

    return bestMatch
}
