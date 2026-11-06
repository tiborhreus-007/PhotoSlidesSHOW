package com.tiborhreus.photoslidesshow.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Vygeneruje sadu ukážkových fotiek (na šírku aj na výšku) do cache priečinka aplikácie,
 * aby bolo možné prototyp okamžite otestovať aj bez povolení alebo na prázdnom zariadení.
 */
object DemoPhotoGenerator {

    private data class Spec(
        val fileName: String,
        val title: String,
        val width: Int,
        val height: Int,
        val topColor: Int,
        val bottomColor: Int,
        val accentColor: Int,
        val daysAgo: Long,
    )

    private const val DAY_MS = 86_400_000L

    private val specs = listOf(
        Spec("01_tatry_sirka.jpg", "Vysoké Tatry – Úsvit", 1600, 1000, 0xFF0F2027.toInt(), 0xFF2C5364.toInt(), 0xFFFFB627.toInt(), 2),
        Spec("02_vodopad_vyska.jpg", "Skok – Horský vodopád", 900, 1400, 0xFF134E5E.toInt(), 0xFF71B280.toInt(), 0xFF4DD8FF.toInt(), 3),
        Spec("03_les_vyska.jpg", "Hmlisté ráno v lese", 900, 1400, 0xFF1F1C2C.toInt(), 0xFF928DAB.toInt(), 0xFFFFD166.toInt(), 5),
        Spec("04_jazero_sirka.jpg", "Štrbské pleso – Zrkadlenie", 1600, 900, 0xFF141E30.toInt(), 0xFF243B55.toInt(), 0xFF38BDF8.toInt(), 12),
        Spec("05_ulica_vyska.jpg", "Nočná ulica v daždi", 900, 1400, 0xFF0F0C29.toInt(), 0xFF302B63.toInt(), 0xFFF472B6.toInt(), 20),
        Spec("06_architektura_vyska.jpg", "Historická veža", 900, 1400, 0xFF232526.toInt(), 0xFF414345.toInt(), 0xFFFBBF24.toInt(), 25),
        Spec("07_zapad_sirka.jpg", "Západ slnka nad hrebeňom", 1600, 1000, 0xFF351C4D.toInt(), 0xFFFF6B6B.toInt(), 0xFFFFE66D.toInt(), 180),
        Spec("08_spomienka_sirka.jpg", "Spomienka – Pred rokom v tento deň", 1600, 900, 0xFF1A2A6C.toInt(), 0xFFB21F1F.toInt(), 0xFFFDBB2D.toInt(), 365),
    )

    fun loadOrCreate(context: Context, sourceOrder: Int): List<PhotoItem> {
        val dir = File(context.cacheDir, "demo_photos").apply { mkdirs() }
        val now = System.currentTimeMillis()
        return specs.map { spec ->
            val file = File(dir, spec.fileName)
            if (!file.exists() || file.length() == 0L) {
                renderDemoBitmap(file, spec)
            }
            PhotoItem(
                uri = Uri.fromFile(file),
                name = "${spec.title} (${if (spec.width >= spec.height) "Na šírku" else "Na výšku"})",
                dateTaken = now - spec.daysAgo * DAY_MS,
                width = spec.width,
                height = spec.height,
                sourceOrder = sourceOrder,
            )
        }
    }

    private fun renderDemoBitmap(target: File, spec: Spec) {
        val bmp = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = spec.width.toFloat()
        val h = spec.height.toFloat()

        // Gradient oblohy
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h, spec.topColor, spec.bottomColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Slnko / mesiac
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.accentColor
            alpha = 215
        }
        canvas.drawCircle(w * 0.72f, h * 0.28f, minOf(w, h) * 0.11f, sunPaint)

        // Silueta hôr
        val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B0C10.toInt()
            alpha = 185
        }
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.65f)
            lineTo(w * 0.28f, h * 0.43f)
            lineTo(w * 0.52f, h * 0.60f)
            lineTo(w * 0.78f, h * 0.38f)
            lineTo(w, h * 0.58f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(path, mountainPaint)

        // Popis a orientácia priamo v obraze
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF4F5F7.toInt()
            textSize = minOf(w, h) * 0.048f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.accentColor
            textSize = minOf(w, h) * 0.034f
        }
        val orientLabel = if (spec.width >= spec.height) {
            "NA ŠÍRKU • ${spec.width}×${spec.height}"
        } else {
            "NA VÝŠKU • ${spec.width}×${spec.height}"
        }
        canvas.drawText(spec.title, w * 0.06f, h * 0.86f, textPaint)
        canvas.drawText(orientLabel, w * 0.06f, h * 0.92f, subPaint)

        FileOutputStream(target).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bmp.recycle()
    }
}
