package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.colorengine.*
import com.example.data.local.entities.DentalCaseEntity
import com.example.data.local.entities.PatientEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class LabReportData(
    val patient: PatientEntity,
    val dentalCase: DentalCaseEntity,
    val dentistName: String = "",
    val clinicName: String = "",
    val primaryShade: ShadeMatchResult,
    val secondShade: ShadeMatchResult?,
    val thirdShade: ShadeMatchResult?,
    val dentistFinalShade: String,
    val overrideReason: String?,
    val overallLab: Lab,
    val zones: ToothZoneShades?,
    val qualityStatus: String,
    val qualityNotes: String,
    val deviceModel: String,
    val cameraId: String,
    val calibrationProfileId: String,
    val algorithmVersion: String,
    val clinicalPhoto: Bitmap? = null
)

object LabReportPdfGenerator {

    /**
     * Generates a professional dental laboratory prescription/shade report PDF.
     * Filename: ShadeReport_<CaseID>_<ToothNumber>_<YYYYMMDD>.pdf
     */
    fun generatePdfReport(context: Context, data: LabReportData): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val readableDate = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.US).format(Date())
        val dateString = dateFormat.format(Date())

        val filename = "ShadeReport_CS-${data.dentalCase.id}_T${data.dentalCase.toothNumber}_${dateString}.pdf"
        val storageDir = File(context.getExternalFilesDir(null), "lab_reports")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val outputFile = File(storageDir, filename)

        val document = PdfDocument()
        // Standard A4 page size in points: 595 x 842 pt
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawPdfContent(canvas, data, readableDate)

        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }

    /**
     * Saves / exports the generated PDF file directly to the device's public Downloads directory.
     * Uses MediaStore on Android 10+ (Q+) for scoped storage compliance, and fallback on earlier versions.
     * Returns the destination path description or null on failure.
     */
    fun savePdfToPublicDownloads(context: Context, sourceFile: File): String? {
        val fileName = sourceFile.name
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DentalReports")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                return "Downloads/DentalReports/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "DentalReports")
                if (!targetDir.exists()) targetDir.mkdirs()
                val destFile = File(targetDir, fileName)

                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                return destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawPdfContent(canvas: Canvas, data: LabReportData, readableDate: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner (Deep Dental Teal)
        paint.color = Color.rgb(13, 92, 117)
        canvas.drawRect(0f, 0f, 595f, 68f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DENTAL SHADE ASSISTANCE REPORT", 28f, 32f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("VITA Classical dataset: REQUIRES_VALIDATED_DATA", 28f, 48f, paint)

        // Clinic Name on Right
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.clinicName, 567f, 30f, paint)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (data.dentistName.isNotBlank()) canvas.drawText("Clinician: ${data.dentistName}", 567f, 44f, paint)
        paint.textAlign = Paint.Align.LEFT

        var y = 78f

        // 2. Patient & Case Metadata Card
        paint.color = Color.rgb(241, 245, 249)
        val metaRect = RectF(28f, y, 567f, y + 62f)
        canvas.drawRoundRect(metaRect, 6f, 6f, paint)

        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(metaRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Metadata grid
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("PATIENT:", 40f, y + 20f, paint)
        canvas.drawText("CASE ID:", 40f, y + 36f, paint)
        canvas.drawText("DATE:", 40f, y + 52f, paint)

        canvas.drawText("TOOTH #:", 210f, y + 20f, paint)
        canvas.drawText("RESTORATION:", 210f, y + 36f, paint)
        canvas.drawText("SHADE SYSTEM:", 210f, y + 52f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${data.patient.name} (${data.patient.patientCode})", 94f, y + 20f, paint)
        canvas.drawText("CS-${data.dentalCase.id}", 94f, y + 36f, paint)
        canvas.drawText(readableDate, 94f, y + 52f, paint)

        canvas.drawText(data.dentalCase.toothNumber, 300f, y + 20f, paint)
        canvas.drawText(data.dentalCase.restorationType, 300f, y + 36f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("VITA Classical A1-D4 (16 Shades)", 300f, y + 52f, paint)

        // Quality Badge on right of card
        val qPass = data.qualityStatus.contains("PASS", ignoreCase = true)
        paint.color = if (qPass) Color.rgb(209, 250, 229) else Color.rgb(254, 243, 199)
        val qBadgeRect = RectF(460f, y + 14f, 555f, y + 48f)
        canvas.drawRoundRect(qBadgeRect, 4f, 4f, paint)
        paint.color = if (qPass) Color.rgb(4, 120, 87) else Color.rgb(180, 83, 9)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("QUALITY AUDIT", 470f, y + 28f, paint)
        paint.textSize = 10f
        canvas.drawText(data.qualityStatus, 470f, y + 42f, paint)

        y += 72f

        // 3. Clinical Photograph & Shade Results Cards
        val photoWidth = 140f
        val photoHeight = 130f
        val photoRect = RectF(28f, y, 28f + photoWidth, y + photoHeight)

        if (data.clinicalPhoto != null) {
            val srcRect = Rect(0, 0, data.clinicalPhoto.width, data.clinicalPhoto.height)
            canvas.drawBitmap(data.clinicalPhoto, srcRect, photoRect, paint)

            paint.color = Color.rgb(148, 163, 184)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(photoRect, paint)
            paint.style = Paint.Style.FILL

            // Caption
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Captured Tooth Photo", 30f, y + photoHeight + 11f, paint)
        } else {
            // Synthetic tooth visualizer with measured enamel color
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(photoRect, paint)
            paint.color = Color.rgb(203, 213, 225)
            paint.style = Paint.Style.STROKE
            canvas.drawRect(photoRect, paint)
            paint.style = Paint.Style.FILL

            val rgb = ColorConversions.labToRgb(data.overallLab)
            paint.color = Color.rgb(rgb.first, rgb.second, rgb.third)
            canvas.drawRoundRect(RectF(38f, y + 14f, 38f + photoWidth - 20f, y + photoHeight - 20f), 12f, 12f, paint)

            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Tooth Enamel Chromatic ROI", 34f, y + photoHeight + 11f, paint)
        }

        // Shade Summary Panels (Right of Photo)
        val shadeBoxX = 180f
        val shadeBoxWidth = 387f

        // A. Primary Recommended Shade Card
        paint.color = Color.rgb(240, 253, 250) // Mint
        val recRect = RectF(shadeBoxX, y, shadeBoxX + shadeBoxWidth, y + 62f)
        canvas.drawRoundRect(recRect, 6f, 6f, paint)

        paint.color = Color.rgb(13, 148, 136)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(recRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(15, 118, 110)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ALGORITHM RECOMMENDED VITA SHADE", shadeBoxX + 12f, y + 18f, paint)

        paint.color = Color.rgb(13, 92, 117)
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.primaryShade.shadeCode, shadeBoxX + 12f, y + 46f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CIEDE2000 ΔE00 = %.2f".format(data.primaryShade.deltaE00), shadeBoxX + 76f, y + 36f, paint)

        val de00Eval = "Numerical comparison only; not clinical validation"
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(de00Eval, shadeBoxX + 76f, y + 48f, paint)

        // Measured vs Ref Color Swatches
        val measuredRgb = ColorConversions.labToRgb(data.overallLab)
        val refRgb = ColorConversions.labToRgb(data.primaryShade.referenceLab)

        paint.color = Color.rgb(measuredRgb.first, measuredRgb.second, measuredRgb.third)
        canvas.drawRoundRect(RectF(shadeBoxX + shadeBoxWidth - 78f, y + 12f, shadeBoxX + shadeBoxWidth - 44f, y + 46f), 4f, 4f, paint)
        paint.color = Color.rgb(refRgb.first, refRgb.second, refRgb.third)
        canvas.drawRoundRect(RectF(shadeBoxX + shadeBoxWidth - 40f, y + 12f, shadeBoxX + shadeBoxWidth - 6f, y + 46f), 4f, 4f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 7f
        canvas.drawText("Tooth", shadeBoxX + shadeBoxWidth - 74f, y + 54f, paint)
        canvas.drawText("VITA", shadeBoxX + shadeBoxWidth - 36f, y + 54f, paint)

        // B. Dentist Final Shade Card
        val finalY = y + 68f
        val isOverridden = data.dentistFinalShade != data.primaryShade.shadeCode
        paint.color = if (isOverridden) Color.rgb(254, 243, 199) else Color.rgb(238, 242, 255)
        val finalRect = RectF(shadeBoxX, finalY, shadeBoxX + shadeBoxWidth, finalY + 62f)
        canvas.drawRoundRect(finalRect, 6f, 6f, paint)

        paint.color = if (isOverridden) Color.rgb(217, 119, 6) else Color.rgb(99, 102, 241)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(finalRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (isOverridden) Color.rgb(180, 83, 9) else Color.rgb(67, 56, 202)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val label = if (isOverridden) "DENTIST FINAL SHADE (CLINICAL OVERRIDE)" else "DENTIST VERIFIED FINAL SHADE"
        canvas.drawText(label, shadeBoxX + 12f, finalY + 18f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.dentistFinalShade, shadeBoxX + 12f, finalY + 46f, paint)

        if (isOverridden && !data.overrideReason.isNullOrBlank()) {
            paint.color = Color.rgb(146, 64, 14)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Override Reason: ${data.overrideReason}", shadeBoxX + 76f, finalY + 38f, paint)
        } else {
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Algorithm recommendation verified and accepted by clinician.", shadeBoxX + 76f, finalY + 38f, paint)
        }

        y += 142f

        // 4. Measured L*a*b* Values Card
        paint.color = Color.rgb(248, 250, 252)
        val labCardRect = RectF(28f, y, 567f, y + 42f)
        canvas.drawRoundRect(labCardRect, 6f, 6f, paint)
        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(labCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ESTIMATED IMAGE COLOR (NOT A CLINICAL MEASUREMENT)", 38f, y + 16f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText(
            "Overall Measured CIE L*a*b*:  L* = %.2f    a* = %.2f    b* = %.2f".format(
                data.overallLab.l, data.overallLab.a, data.overallLab.b
            ),
            38f, y + 32f, paint
        )

        val deltaL = data.overallLab.l - data.primaryShade.referenceLab.l
        val deltaA = data.overallLab.a - data.primaryShade.referenceLab.a
        val deltaB = data.overallLab.b - data.primaryShade.referenceLab.b
        canvas.drawText(
            "Target ${data.primaryShade.shadeCode} Ref: L* = %.2f, a* = %.2f, b* = %.2f (ΔL* %+.2f, Δa* %+.2f, Δb* %+.2f)".format(
                data.primaryShade.referenceLab.l, data.primaryShade.referenceLab.a, data.primaryShade.referenceLab.b,
                deltaL, deltaA, deltaB
            ),
            290f, y + 32f, paint
        )

        y += 50f

        // 5. 2D Color Space Plane Vector Plot (a* vs b* chromaticity diagram drawn directly on PDF canvas!)
        drawPdfColorSpacePlot(canvas, data.overallLab, data.primaryShade, 28f, y, 539f, 120f)

        y += 128f

        // 6. Candidate Ranking Table (Top 3)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Candidate VITA Classical Matches & Perceptibility", 28f, y, paint)

        y += 8f

        // Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(28f, y, 567f, y + 18f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RANK", 38f, y + 12f, paint)
        canvas.drawText("SHADE", 90f, y + 12f, paint)
        canvas.drawText("CIEDE2000 ΔE00", 155f, y + 12f, paint)
        canvas.drawText("UNVERIFIED DATASET LAB", 270f, y + 12f, paint)
        canvas.drawText("INTERPRETATION", 435f, y + 12f, paint)

        y += 18f

        val candidates = listOfNotNull(data.primaryShade, data.secondShade, data.thirdShade)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (cand in candidates) {
            paint.color = if (cand.rank % 2 == 1) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(28f, y, 567f, y + 18f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 8.5f
            canvas.drawText("#${cand.rank}", 38f, y + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(cand.shadeCode, 90f, y + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("%.2f".format(cand.deltaE00), 155f, y + 12f, paint)
            canvas.drawText("L*: %.1f, a*: %.1f, b*: %.1f".format(cand.referenceLab.l, cand.referenceLab.a, cand.referenceLab.b), 270f, y + 12f, paint)

            val interp = "Not clinically validated"
            canvas.drawText(interp, 435f, y + 12f, paint)

            y += 18f
        }

        y += 14f

        // 7. Anatomical Multi-Zone Breakdown (Cervical, Middle, Incisal)
        if (data.zones != null) {
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Anatomical Stratification (Cervical - Middle - Incisal)", 28f, y, paint)
            y += 8f

            val zoneWidth = (567f - 28f) / 3f
            val zoneData = listOf(
                Pair("Cervical Third (Neck)", data.zones.cervical),
                Pair("Middle Third (Body)", data.zones.middle),
                Pair("Incisal Third (Edge)", data.zones.incisal)
            )

            for (i in zoneData.indices) {
                val zx = 28f + i * zoneWidth
                val (zTitle, zInfo) = zoneData[i]

                paint.color = Color.rgb(248, 250, 252)
                val zRect = RectF(zx, y, zx + zoneWidth - 6f, y + 54f)
                canvas.drawRoundRect(zRect, 5f, 5f, paint)

                paint.color = Color.rgb(203, 213, 225)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(zRect, 5f, 5f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.rgb(13, 92, 117)
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(zTitle, zx + 8f, y + 15f, paint)

                paint.color = Color.rgb(15, 23, 42)
                paint.textSize = 14f
                canvas.drawText(zInfo.topMatch.shadeCode, zx + 8f, y + 33f, paint)

                paint.color = Color.rgb(71, 85, 105)
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("ΔE00: %.2f".format(zInfo.topMatch.deltaE00), zx + 44f, y + 30f, paint)
                canvas.drawText("L* %.1f  a* %.1f  b* %.1f".format(zInfo.lab.l, zInfo.lab.a, zInfo.lab.b), zx + 8f, y + 46f, paint)
            }
            y += 64f
        }

        // 8. Laboratory Technician Guide & Clinician Signoff
        paint.color = Color.rgb(241, 245, 249)
        val guideRect = RectF(28f, y, 360f, y + 56f)
        canvas.drawRoundRect(guideRect, 5f, 5f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Ceramic Layering & Verification Notes for Dental Lab", 36f, y + 16f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• Primary porcelain body: VITA Classical ${data.dentistFinalShade}", 36f, y + 28f, paint)
        canvas.drawText("• Cervical: Apply warm chroma dentin; Incisal: Opalescent enamel layer", 36f, y + 39f, paint)
        canvas.drawText("• Perform final shade evaluation under 5500K daylight-balanced illumination", 36f, y + 50f, paint)

        // Clinician Signature Box
        val sigRect = RectF(372f, y, 567f, y + 56f)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(sigRect, 5f, 5f, paint)
        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(sigRect, 5f, 5f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Clinician review (not a signature):", 380f, y + 16f, paint)
        paint.color = Color.rgb(13, 92, 117)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (data.dentistName.isNotBlank()) canvas.drawText(data.dentistName, 380f, y + 34f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 7.5f
        canvas.drawText("Date: $readableDate", 380f, y + 48f, paint)

        // 9. Mandatory Clinical Disclaimer Footer
        val footY = 808f
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(28f, footY - 8f, 567f, footY - 8f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Digital shade-assistance information only. Final clinical shade selection remains the responsibility of the dentist.",
            297.5f,
            footY + 6f,
            paint
        )
        canvas.drawText(
            "Unverified VITA Classical coordinates • Image-based estimate • CIEDE2000 algorithm",
            297.5f,
            footY + 18f,
            paint
        )
    }

    /**
     * Draws the 2D CIELAB a*-b* plane plot directly onto the PDF Canvas.
     */
    private fun drawPdfColorSpacePlot(
        canvas: Canvas,
        scannedLab: Lab,
        targetShade: ShadeMatchResult,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Dark laboratory background
        paint.color = Color.rgb(15, 23, 42)
        val plotRect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(plotRect, 6f, 6f, paint)

        // Title on plot
        paint.color = Color.rgb(226, 232, 240)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2D COLOR SPACE CONFIRMATION: CIELAB (a* vs b* Chromaticity Plane)", x + 12f, y + 14f, paint)

        val padLeft = 40f
        val padRight = 20f
        val padTop = 22f
        val padBottom = 20f

        val innerW = width - padLeft - padRight
        val innerH = height - padTop - padBottom

        val minA = -3.0f
        val maxA = 7.0f
        val minB = 8.0f
        val maxB = 28.0f

        fun map(a: Double, b: Double): PointF {
            val nx = ((a.toFloat() - minA) / (maxA - minA)).coerceIn(0f, 1f)
            val ny = ((b.toFloat() - minB) / (maxB - minB)).coerceIn(0f, 1f)
            val sx = x + padLeft + nx * innerW
            val sy = y + padTop + (1f - ny) * innerH
            return PointF(sx, sy)
        }

        // Draw subtle grid
        paint.color = Color.rgb(30, 41, 59)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        for (i in 0..4) {
            val gx = x + padLeft + (innerW / 4) * i
            canvas.drawLine(gx, y + padTop, gx, y + padTop + innerH, paint)
        }
        for (j in 0..2) {
            val gy = y + padTop + (innerH / 2) * j
            canvas.drawLine(x + padLeft, gy, x + padLeft + innerW, gy, paint)
        }

        // Axis annotations
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("a* axis (Redness →)", x + padLeft + innerW / 2f - 30f, y + padTop + innerH + 12f, paint)
        canvas.drawText("b* (Yellow ↑)", x + 6f, y + padTop + 8f, paint)

        val toothPt = map(scannedLab.a, scannedLab.b)
        val targetPt = map(targetShade.referenceLab.a, targetShade.referenceLab.b)

        // Tolerance ring around target (ΔE = 1.8)
        val pxPerUnit = innerW / (maxA - minA)
        val rad = 1.8f * pxPerUnit * 0.9f
        paint.color = Color.argb(40, 245, 158, 11)
        canvas.drawCircle(targetPt.x, targetPt.y, rad, paint)
        paint.color = Color.rgb(245, 158, 11)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawCircle(targetPt.x, targetPt.y, rad, paint)
        paint.style = Paint.Style.FILL

        // Vector line from tooth to target
        paint.color = Color.rgb(56, 189, 248)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(toothPt.x, toothPt.y, targetPt.x, targetPt.y, paint)
        paint.style = Paint.Style.FILL

        // Plot 16 VITA dots
        VitaClassicalData.ALL_16_SHADES.forEach { ref ->
            val pt = map(ref.lab.a, ref.lab.b)
            val isTarget = ref.shadeCode == targetShade.shadeCode
            val rgb = ColorConversions.labToRgb(ref.lab)

            if (isTarget) {
                paint.color = Color.rgb(245, 158, 11)
                canvas.drawCircle(pt.x, pt.y, 6f, paint)
                paint.color = Color.rgb(rgb.first, rgb.second, rgb.third)
                canvas.drawCircle(pt.x, pt.y, 4f, paint)

                paint.color = Color.rgb(253, 230, 138)
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(ref.shadeCode, pt.x + 6f, pt.y + 3f, paint)
            } else {
                paint.color = Color.rgb(71, 85, 105)
                canvas.drawCircle(pt.x, pt.y, 3.5f, paint)
                paint.color = Color.rgb(rgb.first, rgb.second, rgb.third)
                canvas.drawCircle(pt.x, pt.y, 2.5f, paint)

                paint.color = Color.rgb(148, 163, 184)
                paint.textSize = 6.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(ref.shadeCode, pt.x + 4f, pt.y + 2f, paint)
            }
        }

        // Plot Scanned Tooth Reticle
        paint.color = Color.rgb(56, 189, 248)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(toothPt.x - 8f, toothPt.y, toothPt.x + 8f, toothPt.y, paint)
        canvas.drawLine(toothPt.x, toothPt.y - 8f, toothPt.x, toothPt.y + 8f, paint)
        canvas.drawCircle(toothPt.x, toothPt.y, 5f, paint)

        val toothRgb = ColorConversions.labToRgb(scannedLab)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(toothRgb.first, toothRgb.second, toothRgb.third)
        canvas.drawCircle(toothPt.x, toothPt.y, 3.5f, paint)

        paint.color = Color.rgb(56, 189, 248)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Scanned Tooth", toothPt.x - 22f, toothPt.y - 8f, paint)
    }
}
