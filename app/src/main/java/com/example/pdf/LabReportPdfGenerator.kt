package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.colorengine.Lab
import com.example.colorengine.ShadeMatchResult
import com.example.colorengine.ToothZoneShades
import com.example.data.local.entities.DentalCaseEntity
import com.example.data.local.entities.PatientEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class LabReportData(
    val patient: PatientEntity,
    val dentalCase: DentalCaseEntity,
    val dentistName: String = "Dr. Alexander Wright, DDS",
    val clinicName: String = "Apex Dental Aesthetics & Prosthodontics",
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
     * Filename: ShadeReport_<CaseID>_<YYYYMMDD>.pdf
     */
    fun generatePdfReport(context: Context, data: LabReportData): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val readableDate = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.US).format(Date())
        val dateString = dateFormat.format(Date())

        val filename = "ShadeReport_${data.dentalCase.id}_${dateString}.pdf"
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

    private fun drawPdfContent(canvas: Canvas, data: LabReportData, readableDate: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner (Deep Dental Teal)
        paint.color = Color.rgb(13, 92, 117)
        canvas.drawRect(0f, 0f, 595f, 75f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DENTAL SHADE REPORT", 32f, 38f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Digital Lab Prescription & Colorimetric Verification", 32f, 55f, paint)

        // Clinic Name on Right
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.clinicName, 563f, 36f, paint)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Attending Clinician: ${data.dentistName}", 563f, 52f, paint)
        paint.textAlign = Paint.Align.LEFT

        var y = 100f

        // 2. Patient & Case Metadata Card
        paint.color = Color.rgb(241, 245, 249)
        val metaRect = RectF(32f, y, 563f, y + 80f)
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Metadata grid
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("CASE ID:", 46f, y + 24f, paint)
        canvas.drawText("PATIENT:", 46f, y + 44f, paint)
        canvas.drawText("RECORD DATE:", 46f, y + 64f, paint)

        canvas.drawText("TOOTH #:", 220f, y + 24f, paint)
        canvas.drawText("RESTORATION:", 220f, y + 44f, paint)
        canvas.drawText("SHADE SYSTEM:", 220f, y + 64f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("CS-${data.dentalCase.id}", 110f, y + 24f, paint)
        canvas.drawText("${data.patient.name} (${data.patient.patientCode})", 110f, y + 44f, paint)
        canvas.drawText(readableDate, 110f, y + 64f, paint)

        canvas.drawText(data.dentalCase.toothNumber, 310f, y + 24f, paint)
        canvas.drawText(data.dentalCase.restorationType, 310f, y + 44f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("VITA_CLASSICAL (16-Shade)", 310f, y + 64f, paint)

        y += 100f

        // 3. Clinical Photograph & Shade Results Side by Side
        val photoWidth = 160f
        val photoHeight = 160f
        if (data.clinicalPhoto != null) {
            val destRect = RectF(32f, y, 32f + photoWidth, y + photoHeight)
            val srcRect = Rect(0, 0, data.clinicalPhoto.width, data.clinicalPhoto.height)
            canvas.drawBitmap(data.clinicalPhoto, srcRect, destRect, paint)

            paint.color = Color.rgb(148, 163, 184)
            paint.style = Paint.Style.STROKE
            canvas.drawRect(destRect, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 8f
            canvas.drawText("Clinical Enamel ROI", 36f, y + photoHeight + 14f, paint)
        } else {
            // Placeholder box
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(32f, y, 32f + photoWidth, y + photoHeight, paint)
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 9f
            canvas.drawText("Clinical Image Registered", 46f, y + 80f, paint)
        }

        // Shade Summary Box (Right side of Photo)
        val shadeBoxX = 210f
        val shadeBoxWidth = 353f

        // Recommended Shade card
        paint.color = Color.rgb(240, 253, 250) // Soft mint teal
        val recRect = RectF(shadeBoxX, y, shadeBoxX + shadeBoxWidth, y + 74f)
        canvas.drawRoundRect(recRect, 6f, 6f, paint)

        paint.color = Color.rgb(13, 148, 136)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(recRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(15, 118, 110)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ALGORITHM RECOMMENDED SHADE", shadeBoxX + 14f, y + 20f, paint)

        paint.color = Color.rgb(13, 92, 117)
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.primaryShade.shadeCode, shadeBoxX + 14f, y + 54f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("CIEDE2000 ΔE00 = %.2f".format(data.primaryShade.deltaE00), shadeBoxX + 90f, y + 42f, paint)

        val de00Eval = if (data.primaryShade.deltaE00 <= 1.8) "Clinically Valid Match (ΔE00 ≤ 1.8)" else "Moderate Deviation (ΔE00 > 1.8)"
        canvas.drawText(de00Eval, shadeBoxX + 90f, y + 56f, paint)

        // Dentist Final Shade Box
        val finalY = y + 84f
        val isOverridden = data.dentistFinalShade != data.primaryShade.shadeCode
        paint.color = if (isOverridden) Color.rgb(254, 243, 199) else Color.rgb(238, 242, 255)
        val finalRect = RectF(shadeBoxX, finalY, shadeBoxX + shadeBoxWidth, finalY + 76f)
        canvas.drawRoundRect(finalRect, 6f, 6f, paint)

        paint.color = if (isOverridden) Color.rgb(217, 119, 6) else Color.rgb(99, 102, 241)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(finalRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (isOverridden) Color.rgb(180, 83, 9) else Color.rgb(67, 56, 202)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val label = if (isOverridden) "DENTIST FINAL SHADE (CLINICAL OVERRIDE)" else "DENTIST CONFIRMED FINAL SHADE"
        canvas.drawText(label, shadeBoxX + 14f, finalY + 20f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.dentistFinalShade, shadeBoxX + 14f, finalY + 54f, paint)

        if (isOverridden && !data.overrideReason.isNullOrBlank()) {
            paint.color = Color.rgb(146, 64, 14)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Reason: ${data.overrideReason}", shadeBoxX + 90f, finalY + 48f, paint)
        } else {
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Algorithm recommendation accepted by clinician.", shadeBoxX + 90f, finalY + 48f, paint)
        }

        y += 185f

        // 4. Candidate Ranking & Multi-zone Table
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Top 3 Candidate VITA Classical Matches", 32f, y, paint)

        y += 14f

        // Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(32f, y, 563f, y + 20f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RANK", 42f, y + 14f, paint)
        canvas.drawText("SHADE", 100f, y + 14f, paint)
        canvas.drawText("CIEDE2000 ΔE00", 170f, y + 14f, paint)
        canvas.drawText("REFERENCE LAB (D65/2°)", 290f, y + 14f, paint)
        canvas.drawText("CLINICAL PERCEPTIBILITY", 440f, y + 14f, paint)

        y += 20f

        val candidates = listOfNotNull(data.primaryShade, data.secondShade, data.thirdShade)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (cand in candidates) {
            paint.color = if (cand.rank % 2 == 1) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(32f, y, 563f, y + 20f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 9f
            canvas.drawText("#${cand.rank}", 42f, y + 14f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(cand.shadeCode, 100f, y + 14f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("%.2f".format(cand.deltaE00), 170f, y + 14f, paint)
            canvas.drawText("L*: %.1f, a*: %.1f, b*: %.1f".format(cand.referenceLab.l, cand.referenceLab.a, cand.referenceLab.b), 290f, y + 14f, paint)

            val interp = when {
                cand.deltaE00 <= 0.8 -> "Imperceptible (ΔE ≤ 0.8)"
                cand.deltaE00 <= 1.8 -> "Clinically Acceptable (ΔE ≤ 1.8)"
                cand.deltaE00 <= 3.2 -> "Moderately Acceptable"
                else -> "Noticeable Discrepancy"
            }
            canvas.drawText(interp, 440f, y + 14f, paint)

            y += 20f
        }

        y += 18f

        // 5. Anatomical Tooth Zones (Cervical, Middle, Incisal)
        if (data.zones != null) {
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Anatomical Stratification (Cervical - Middle - Incisal)", 32f, y, paint)
            y += 14f

            val zoneWidth = (563f - 32f) / 3f
            val zoneData = listOf(
                Pair("Cervical (Neck)", data.zones.cervical),
                Pair("Middle (Body)", data.zones.middle),
                Pair("Incisal (Edge)", data.zones.incisal)
            )

            for (i in zoneData.indices) {
                val zx = 32f + i * zoneWidth
                val (zTitle, zInfo) = zoneData[i]

                paint.color = Color.rgb(248, 250, 252)
                val zRect = RectF(zx, y, zx + zoneWidth - 8f, y + 64f)
                canvas.drawRoundRect(zRect, 6f, 6f, paint)

                paint.color = Color.rgb(203, 213, 225)
                paint.style = Paint.Style.STROKE
                canvas.drawRoundRect(zRect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.rgb(13, 92, 117)
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(zTitle, zx + 10f, y + 18f, paint)

                paint.color = Color.rgb(15, 23, 42)
                paint.textSize = 16f
                canvas.drawText(zInfo.topMatch.shadeCode, zx + 10f, y + 40f, paint)

                paint.color = Color.rgb(71, 85, 105)
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("ΔE00: %.2f".format(zInfo.topMatch.deltaE00), zx + 46f, y + 36f, paint)
                canvas.drawText("L* %.1f a* %.1f b* %.1f".format(zInfo.lab.l, zInfo.lab.a, zInfo.lab.b), zx + 10f, y + 54f, paint)
            }
            y += 80f
        }

        // 6. Quality & Calibration Audit Information
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Measurement Quality & Device Traceability", 32f, y, paint)
        y += 14f

        paint.color = Color.rgb(241, 245, 249)
        val auditRect = RectF(32f, y, 563f, y + 60f)
        canvas.drawRoundRect(auditRect, 6f, 6f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Quality Status: ${data.qualityStatus}", 44f, y + 18f, paint)
        canvas.drawText("Algorithm Version: ${data.algorithmVersion}", 44f, y + 32f, paint)
        canvas.drawText("Device Model: ${data.deviceModel}", 44f, y + 46f, paint)

        canvas.drawText("Overall Measured: L* %.2f, a* %.2f, b* %.2f".format(data.overallLab.l, data.overallLab.a, data.overallLab.b), 280f, y + 18f, paint)
        canvas.drawText("Calibration Profile: ${data.calibrationProfileId}", 280f, y + 32f, paint)
        canvas.drawText("Camera ID: ${data.cameraId}", 280f, y + 46f, paint)

        // 7. Mandatory Legal & Clinical Footer (Per Prompt Section 22)
        y = 800f
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(32f, y - 10f, 563f, y - 10f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Digital shade-assistance information only. Final clinical shade selection remains the responsibility of the dentist.",
            297.5f,
            y + 6f,
            paint
        )
        canvas.drawText(
            "Generated by VITA Dental Shade Mobile System • Non-spectrophotometric optical estimate",
            297.5f,
            y + 18f,
            paint
        )
    }
}
