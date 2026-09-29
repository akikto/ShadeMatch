package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.colorengine.*
import kotlin.math.*

enum class ComparisonDisplayMode {
    SIDE_BY_SIDE,
    SPLIT_BISECTED,
    ZONE_STRATIFIED,
    COLOR_SPACE_2D
}

@Composable
fun ShadeComparisonCard(
    scannedLab: Lab,
    topCandidates: List<ShadeMatchResult>,
    clinicalPhoto: Bitmap? = null,
    cervicalLab: Lab? = null,
    middleLab: Lab? = null,
    incisalLab: Lab? = null,
    modifier: Modifier = Modifier
) {
    var selectedReferenceCode by remember(topCandidates) {
        mutableStateOf(topCandidates.firstOrNull()?.shadeCode ?: "A2")
    }
    var displayMode by remember { mutableStateOf(ComparisonDisplayMode.SIDE_BY_SIDE) }
    var isFullScreenOpen by remember { mutableStateOf(false) }

    val activeRef = VitaClassicalData.getByCode(selectedReferenceCode)
        ?: VitaClassicalData.ALL_16_SHADES.first()
    val deltaE00 = remember(scannedLab, activeRef.lab) {
        Ciede2000.calculate(scannedLab, activeRef.lab)
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("shade_comparison_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Title and Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Visual Shade Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Scanned tooth color vs. VITA reference standard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { isFullScreenOpen = true },
                    modifier = Modifier.testTag("expand_comparison_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen Comparison",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reference Tabs Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                topCandidates.take(3).forEach { cand ->
                    val isSelected = selectedReferenceCode == cand.shadeCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedReferenceCode = cand.shadeCode },
                        label = {
                            Text(
                                text = "${cand.shadeCode} (ΔE %.2f)".format(cand.deltaE00),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("compare_chip_${cand.shadeCode}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented Mode Buttons
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = displayMode == ComparisonDisplayMode.SIDE_BY_SIDE,
                    onClick = { displayMode = ComparisonDisplayMode.SIDE_BY_SIDE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                ) {
                    Text("Side-by-Side", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = displayMode == ComparisonDisplayMode.SPLIT_BISECTED,
                    onClick = { displayMode = ComparisonDisplayMode.SPLIT_BISECTED },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                ) {
                    Text("Split", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = displayMode == ComparisonDisplayMode.ZONE_STRATIFIED,
                    onClick = { displayMode = ComparisonDisplayMode.ZONE_STRATIFIED },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                ) {
                    Text("Zones", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = displayMode == ComparisonDisplayMode.COLOR_SPACE_2D,
                    onClick = { displayMode = ComparisonDisplayMode.COLOR_SPACE_2D },
                    shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                ) {
                    Text("2D Plane", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Visual View according to selected mode
            when (displayMode) {
                ComparisonDisplayMode.SIDE_BY_SIDE -> {
                    SideBySideView(
                        scannedLab = scannedLab,
                        referenceRef = activeRef,
                        deltaE00 = deltaE00,
                        clinicalPhoto = clinicalPhoto
                    )
                }
                ComparisonDisplayMode.SPLIT_BISECTED -> {
                    BisectedToothView(
                        scannedLab = scannedLab,
                        referenceLab = activeRef.lab,
                        referenceCode = activeRef.shadeCode,
                        deltaE00 = deltaE00
                    )
                }
                ComparisonDisplayMode.ZONE_STRATIFIED -> {
                    ZoneStratifiedComparisonView(
                        scannedOverallLab = scannedLab,
                        cervicalLab = cervicalLab,
                        middleLab = middleLab,
                        incisalLab = incisalLab,
                        referenceRef = activeRef
                    )
                }
                ComparisonDisplayMode.COLOR_SPACE_2D -> {
                    ColorSpacePlaneCanvas(
                        scannedLab = scannedLab,
                        targetShade = activeRef.shadeCode,
                        onSelectShade = { selectedReferenceCode = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Colorimetric Delta breakdown table
            ColorimetricDeltaBreakdown(scannedLab = scannedLab, referenceLab = activeRef.lab, deltaE00 = deltaE00)
        }
    }

    // Full Screen Interactive Modal
    if (isFullScreenOpen) {
        FullScreenComparisonDialog(
            scannedLab = scannedLab,
            topCandidates = topCandidates,
            clinicalPhoto = clinicalPhoto,
            cervicalLab = cervicalLab,
            middleLab = middleLab,
            incisalLab = incisalLab,
            initialShade = selectedReferenceCode,
            onDismiss = { isFullScreenOpen = false }
        )
    }
}

/**
 * Side-by-Side Dual Panel Comparison
 */
@Composable
fun SideBySideView(
    scannedLab: Lab,
    referenceRef: VitaReference,
    deltaE00: Double,
    clinicalPhoto: Bitmap?
) {
    val scannedRgb = ColorConversions.labToRgb(scannedLab)
    val refRgb = ColorConversions.labToRgb(referenceRef.lab)

    val scannedColor = Color(scannedRgb.first, scannedRgb.second, scannedRgb.third)
    val refColor = Color(refRgb.first, refRgb.second, refRgb.third)

    Row(
        modifier = Modifier.fillMaxWidth().height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Scanned Tooth Panel (Left)
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCANNED TOOTH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Tooth Color Visualization
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scannedColor)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AnatomicalToothSilhouette(color = scannedColor, modifier = Modifier.size(80.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Calibrated Optical Color",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "L* %.1f  a* %.1f  b* %.1f".format(scannedLab.l, scannedLab.a, scannedLab.b),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Center Delta Indicator Bar
        Column(
            modifier = Modifier.width(36.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = when {
                    deltaE00 <= 0.8 -> Color(0xFFD1FAE5) // Green
                    deltaE00 <= 1.8 -> Color(0xFFE0F2FE) // Light Blue
                    deltaE00 <= 3.2 -> Color(0xFFFEF3C7) // Amber
                    else -> Color(0xFFFEE2E2) // Red
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "ΔE\n%.1f".format(deltaE00),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        deltaE00 <= 0.8 -> Color(0xFF065F46)
                        deltaE00 <= 1.8 -> Color(0xFF0369A1)
                        deltaE00 <= 3.2 -> Color(0xFF92400E)
                        else -> Color(0xFF991B1B)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    lineHeight = 11.sp
                )
            }
        }

        // VITA Classical Reference Panel (Right)
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VITA ${referenceRef.shadeCode}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D5C75)
                    )
                    Surface(
                        color = Color(0xFFD0F0FD),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Reference",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF063342),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // VITA Tab Visualization
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(refColor)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AnatomicalToothSilhouette(color = refColor, modifier = Modifier.size(80.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = referenceRef.hueGroup,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "L* %.1f  a* %.1f  b* %.1f".format(referenceRef.lab.l, referenceRef.lab.a, referenceRef.lab.b),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Bisected Split Tooth View with interactive slider divider
 */
@Composable
fun BisectedToothView(
    scannedLab: Lab,
    referenceLab: Lab,
    referenceCode: String,
    deltaE00: Double
) {
    val scannedRgb = ColorConversions.labToRgb(scannedLab)
    val refRgb = ColorConversions.labToRgb(referenceLab)

    val scannedColor = Color(scannedRgb.first, scannedRgb.second, scannedRgb.third)
    val refColor = Color(refRgb.first, refRgb.second, refRgb.third)

    var splitPosition by remember { mutableStateOf(0.5f) } // 0.0 to 1.0

    Column(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(170.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newPos = (change.position.x / size.width).coerceIn(0.1f, 0.9f)
                        splitPosition = newPos
                    }
                }
        ) {
            // Draw Split Tooth Crown
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val w = size.width * 0.72f
                val h = size.height * 0.88f
                val splitX = size.width * splitPosition

                val toothPath = createToothSilhouettePath(cx, cy, w, h)

                // 1. Draw Left half (Scanned Tooth)
                clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                    drawPath(toothPath, color = scannedColor)
                }

                // 2. Draw Right half (Reference VITA Tab)
                clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                    drawPath(toothPath, color = refColor)
                }

                // Outline
                drawPath(toothPath, color = Color.Black.copy(alpha = 0.25f), style = Stroke(width = 1.5.dp.toPx()))

                // Vertical Divider Line
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, 8f),
                    end = Offset(splitX, size.height - 8f),
                    strokeWidth = 2.5.dp.toPx()
                )

                // Slider Handle Handle Circle
                drawCircle(
                    color = Color(0xFF0D5C75),
                    radius = 9.dp.toPx(),
                    center = Offset(splitX, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(splitX, cy)
                )
            }

            // Labels overlaid
            Text(
                text = "Scanned",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp)
            )

            Text(
                text = "VITA $referenceCode",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Drag slider across seam line to inspect color gradient & edge metamerism",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

/**
 * Anatomical Zone Stratified View (Cervical, Middle, Incisal)
 */
@Composable
fun ZoneStratifiedComparisonView(
    scannedOverallLab: Lab,
    cervicalLab: Lab?,
    middleLab: Lab?,
    incisalLab: Lab?,
    referenceRef: VitaReference
) {
    val zones = listOf(
        Triple("Cervical (Neck)", cervicalLab ?: Lab(scannedOverallLab.l - 2.5, scannedOverallLab.a + 0.8, scannedOverallLab.b + 3.2), "Warmer Dentin"),
        Triple("Middle (Body)", middleLab ?: scannedOverallLab, "Diagnostic Zone"),
        Triple("Incisal (Edge)", incisalLab ?: Lab(scannedOverallLab.l + 2.0, scannedOverallLab.a - 0.6, scannedOverallLab.b - 2.8), "Enamel Translucency")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        zones.forEach { (zoneTitle, measuredLab, note) ->
            val zoneDe00 = Ciede2000.calculate(measuredLab, referenceRef.lab)
            val measuredRgb = ColorConversions.labToRgb(measuredLab)
            val refRgb = ColorConversions.labToRgb(referenceRef.lab)

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = zoneTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(text = note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Dual Color Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(measuredRgb.first, measuredRgb.second, measuredRgb.third))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            )
                            Text("Tooth", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(refRgb.first, refRgb.second, refRgb.third))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            )
                            Text(referenceRef.shadeCode, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Delta Value
                    Surface(
                        color = if (zoneDe00 <= 1.8) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "ΔE %.2f".format(zoneDe00),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (zoneDe00 <= 1.8) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Colorimetric Delta Parameter Breakdown (ΔL*, ΔC*, ΔH*, ΔE00)
 */
@Composable
fun ColorimetricDeltaBreakdown(
    scannedLab: Lab,
    referenceLab: Lab,
    deltaE00: Double
) {
    val deltaL = scannedLab.l - referenceLab.l
    val cScanned = sqrt(scannedLab.a.pow(2) + scannedLab.b.pow(2))
    val cRef = sqrt(referenceLab.a.pow(2) + referenceLab.b.pow(2))
    val deltaC = cScanned - cRef

    val deltaA = scannedLab.a - referenceLab.a
    val deltaB = scannedLab.b - referenceLab.b
    val deltaHDiffSq = max(0.0, deltaA.pow(2) + deltaB.pow(2) - deltaC.pow(2))
    val deltaH = sqrt(deltaHDiffSq)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Color Coordinate Deviations (CIELAB Space)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeltaMetricCell(
                    symbol = "ΔL*",
                    name = "Lightness",
                    value = deltaL,
                    note = if (deltaL > 0) "Tooth lighter" else "Tooth darker"
                )
                DeltaMetricCell(
                    symbol = "ΔC*",
                    name = "Chroma",
                    value = deltaC,
                    note = if (deltaC > 0) "Higher saturation" else "Lower saturation"
                )
                DeltaMetricCell(
                    symbol = "ΔH*",
                    name = "Hue Shift",
                    value = deltaH,
                    note = if (deltaH < 1.0) "Minimal hue shift" else "Noticeable tint"
                )
                DeltaMetricCell(
                    symbol = "ΔE00",
                    name = "Overall",
                    value = deltaE00,
                    note = if (deltaE00 <= 1.8) "Clinically Valid" else "Review advised",
                    isHero = true
                )
            }
        }
    }
}

@Composable
fun DeltaMetricCell(
    symbol: String,
    name: String,
    value: Double,
    note: String,
    isHero: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = (if (value > 0 && !isHero) "+" else "") + "%.2f".format(value),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = if (isHero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = name,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Full Screen Comparison Dialog
 */
@Composable
fun FullScreenComparisonDialog(
    scannedLab: Lab,
    topCandidates: List<ShadeMatchResult>,
    clinicalPhoto: Bitmap?,
    cervicalLab: Lab?,
    middleLab: Lab?,
    incisalLab: Lab?,
    initialShade: String,
    onDismiss: () -> Unit
) {
    var selectedShade by remember { mutableStateOf(initialShade) }
    var activeMode by remember { mutableStateOf(ComparisonDisplayMode.SIDE_BY_SIDE) }

    val activeRef = VitaClassicalData.getByCode(selectedShade) ?: VitaClassicalData.ALL_16_SHADES.first()
    val deltaE00 = Ciede2000.calculate(scannedLab, activeRef.lab)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top App Bar in Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Side-by-Side Shade Verification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scanned Tooth vs. VITA Classical $selectedShade (ΔE00 = %.2f)".format(deltaE00),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 16 VITA Tabs Grid for Instant Cross-Comparison
                Text(
                    text = "Select VITA Classical Reference Standard:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rows = VitaClassicalData.ALL_16_SHADES.chunked(8)
                    rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { ref ->
                                val isSelected = selectedShade == ref.shadeCode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedShade = ref.shadeCode }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ref.shadeCode,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Mode Tabs
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeMode == ComparisonDisplayMode.SIDE_BY_SIDE,
                        onClick = { activeMode = ComparisonDisplayMode.SIDE_BY_SIDE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Side-by-Side")
                    }
                    SegmentedButton(
                        selected = activeMode == ComparisonDisplayMode.SPLIT_BISECTED,
                        onClick = { activeMode = ComparisonDisplayMode.SPLIT_BISECTED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Split Slider")
                    }
                    SegmentedButton(
                        selected = activeMode == ComparisonDisplayMode.ZONE_STRATIFIED,
                        onClick = { activeMode = ComparisonDisplayMode.ZONE_STRATIFIED },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Zones")
                    }
                }

                // Visualization Area
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                        when (activeMode) {
                            ComparisonDisplayMode.SIDE_BY_SIDE -> {
                                SideBySideView(
                                    scannedLab = scannedLab,
                                    referenceRef = activeRef,
                                    deltaE00 = deltaE00,
                                    clinicalPhoto = clinicalPhoto
                                )
                            }
                            ComparisonDisplayMode.SPLIT_BISECTED -> {
                                BisectedToothView(
                                    scannedLab = scannedLab,
                                    referenceLab = activeRef.lab,
                                    referenceCode = activeRef.shadeCode,
                                    deltaE00 = deltaE00
                                )
                            }
                            ComparisonDisplayMode.ZONE_STRATIFIED -> {
                                ZoneStratifiedComparisonView(
                                    scannedOverallLab = scannedLab,
                                    cervicalLab = cervicalLab,
                                    middleLab = middleLab,
                                    incisalLab = incisalLab,
                                    referenceRef = activeRef
                                )
                            }
                        }
                    }
                }

                // Delta breakdown
                ColorimetricDeltaBreakdown(scannedLab = scannedLab, referenceLab = activeRef.lab, deltaE00 = deltaE00)

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply & Close Verification")
                }
            }
        }
    }
}

/**
 * Renders an anatomical tooth silhouette with realistic tooth contour
 */
@Composable
fun AnatomicalToothSilhouette(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val w = size.width * 0.72f
        val h = size.height * 0.88f

        val toothPath = createToothSilhouettePath(cx, cy, w, h)
        drawPath(toothPath, color = color)
        drawPath(toothPath, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.dp.toPx()))
    }
}

private fun createToothSilhouettePath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val topY = cy - h / 2f
    val botY = cy + h / 2f
    val halfW = w / 2f

    return Path().apply {
        moveTo(cx - halfW * 0.8f, topY)
        cubicTo(
            cx - halfW * 1.15f, cy - h * 0.1f,
            cx - halfW * 1.10f, cy + h * 0.3f,
            cx - halfW, botY
        )
        // Incisal edge
        cubicTo(
            cx - halfW * 0.5f, botY + 4f,
            cx + halfW * 0.5f, botY + 4f,
            cx + halfW, botY
        )
        cubicTo(
            cx + halfW * 1.10f, cy + h * 0.3f,
            cx + halfW * 1.15f, cy - h * 0.1f,
            cx + halfW * 0.8f, topY
        )
        // Cervical arch
        cubicTo(
            cx + halfW * 0.4f, topY - 8f,
            cx - halfW * 0.4f, topY - 8f,
            cx - halfW * 0.8f, topY
        )
        close()
    }
}
