package com.example.vision

import android.graphics.*
import com.example.colorengine.ColorConversions
import com.example.colorengine.Lab
import com.example.colorengine.VitaClassicalData
import kotlin.math.sin

object DentalImageHelper {

    /**
     * Generates a high-fidelity synthetic clinical dental photograph for testing and calibration.
     * Features: realistic tooth crown shape, cervical-to-incisal shade gradient, enamel translucency,
     * anatomical gingival margin, and realistic oral cavity background.
     */
    fun generateSimulatedToothImage(
        targetVitaShade: String = "A2",
        width: Int = 400,
        height: Int = 400,
        includeSpecularHighlight: Boolean = true,
        blurRadius: Float = 0f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Dark oral cavity background
        val bgPaint = Paint().apply {
            color = Color.rgb(20, 16, 18)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Gingival margin (pink/red gum tissue at top)
        val gingivaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 75, 85)
        }
        val gingivaPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(width.toFloat(), 0f)
            lineTo(width.toFloat(), height * 0.28f)
            cubicTo(
                width * 0.75f, height * 0.22f,
                width * 0.25f, height * 0.22f,
                0f, height * 0.28f
            )
            close()
        }
        canvas.drawPath(gingivaPath, gingivaPaint)

        // 3. Central Tooth Crown (Maxillary Incisor)
        val baseRef = VitaClassicalData.getByCode(targetVitaShade) ?: VitaClassicalData.ALL_16_SHADES[1]
        val midLab = baseRef.lab

        // Cervical is slightly warmer/yellower (+3 b*, -2 L*)
        val cervLab = Lab(midLab.l - 2.5, midLab.a + 0.8, midLab.b + 3.2)
        // Incisal is more translucent/cooler (+2 L*, -3 b*)
        val incLab = Lab(midLab.l + 2.0, midLab.a - 0.6, midLab.b - 2.8)

        val cervRgb = ColorConversions.labToRgb(cervLab)
        val midRgb = ColorConversions.labToRgb(midLab)
        val incRgb = ColorConversions.labToRgb(incLab)

        val toothShader = LinearGradient(
            width * 0.5f, height * 0.24f,
            width * 0.5f, height * 0.84f,
            intArrayOf(
                Color.rgb(cervRgb.first, cervRgb.second, cervRgb.third),
                Color.rgb(midRgb.first, midRgb.second, midRgb.third),
                Color.rgb(incRgb.first, incRgb.second, incRgb.third)
            ),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val toothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = toothShader
        }

        val toothPath = Path().apply {
            val cx = width * 0.5f
            val topY = height * 0.24f
            val botY = height * 0.82f
            val halfW = width * 0.22f

            moveTo(cx - halfW * 0.85f, topY)
            cubicTo(
                cx - halfW * 1.15f, height * 0.45f,
                cx - halfW * 1.10f, height * 0.70f,
                cx - halfW, botY
            )
            // Incisal edge (slight mamelon curvature)
            cubicTo(
                cx - halfW * 0.5f, botY + 4f,
                cx + halfW * 0.5f, botY + 4f,
                cx + halfW, botY
            )
            cubicTo(
                cx + halfW * 1.10f, height * 0.70f,
                cx + halfW * 1.15f, height * 0.45f,
                cx + halfW * 0.85f, topY
            )
            // Cervical margin arch
            cubicTo(
                cx + halfW * 0.4f, topY - 12f,
                cx - halfW * 0.4f, topY - 12f,
                cx - halfW * 0.85f, topY
            )
            close()
        }
        canvas.drawPath(toothPath, toothPaint)

        // 4. Subtle anatomical line angles & developmental lobes
        val lobePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(25, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawLine(width * 0.44f, height * 0.40f, width * 0.42f, height * 0.75f, lobePaint)
        canvas.drawLine(width * 0.56f, height * 0.40f, width * 0.58f, height * 0.75f, lobePaint)

        // 5. Specular highlight (saliva reflection) if requested
        if (includeSpecularHighlight) {
            val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(160, 255, 255, 255)
            }
            canvas.drawOval(
                RectF(width * 0.42f, height * 0.45f, width * 0.48f, height * 0.49f),
                specularPaint
            )
        }

        return bitmap
    }
}
