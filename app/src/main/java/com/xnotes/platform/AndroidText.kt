package com.xnotes.platform

import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import com.xnotes.core.pal.FontFace
import com.xnotes.core.pal.FontSpec

/**
 * Shared text layout so measuring and drawing produce identical results
 * (spec 01 §12). A "point size" is converted to page-space pixels at the
 * document authoring DPI (150), so 13pt renders as true 13pt on a 150-DPI page.
 */
object AndroidText {
    /** points -> page pixels at 150 DPI (1pt = 1/72 inch). */
    const val POINTS_TO_PX = 150f / 72f

    // The four abstract faces, resolved to platform typefaces. Held as immutable
    // vals (no shared mutable cache) since paints are built on background cache threads.
    private val sans: Typeface = Typeface.SANS_SERIF
    private val serif: Typeface = Typeface.SERIF
    private val mono: Typeface = Typeface.MONOSPACE
    private val hand: Typeface = Typeface.create("cursive", Typeface.NORMAL)

    private val customFonts = java.util.concurrent.ConcurrentHashMap<String, Typeface>()

    fun initCustomFonts(context: android.content.Context) {
        val fontsDir = java.io.File(context.filesDir, "fonts")
        if (!fontsDir.exists()) return
        val files = fontsDir.listFiles() ?: return
        val newCustomFonts = mutableMapOf<String, Typeface>()
        for (file in files) {
            if (file.isFile && (file.name.endsWith(".ttf", ignoreCase = true) || file.name.endsWith(".otf", ignoreCase = true))) {
                try {
                    val typeface = Typeface.createFromFile(file)
                    val id = file.nameWithoutExtension
                    newCustomFonts[id] = typeface
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        synchronized(customFonts) {
            customFonts.clear()
            customFonts.putAll(newCustomFonts)
            // Re-populate FontFace's customFaces so the UI editor shows them
            com.xnotes.core.pal.FontFace.clearCustomFaces()
            for (id in newCustomFonts.keys) {
                com.xnotes.core.pal.FontFace.fromId(id)
            }
        }
    }

    fun getTypeface(face: FontFace): Typeface = base(face)

    private fun base(face: FontFace): Typeface = when (face) {
        FontFace.SANS -> sans
        FontFace.SERIF -> serif
        FontFace.MONO -> mono
        FontFace.HAND -> hand
        else -> customFonts[face.id] ?: mono
    }

    fun textPaint(font: FontSpec, argb: Int = 0xFF000000.toInt()): TextPaint =
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            val face = base(font.face)
            // Typeface.create(base, BOLD) is thread-safe and framework-cached.
            typeface = if (font.bold) Typeface.create(face, Typeface.BOLD) else face
            textSize = (font.pointSize * POINTS_TO_PX).toFloat()
            color = argb
        }

    fun layout(text: CharSequence, widthPx: Int, paint: TextPaint): StaticLayout =
        StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(1))
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()

    fun lineHeight(font: FontSpec): Double {
        val fm = textPaint(font).fontMetrics
        return (fm.descent - fm.ascent).toDouble()
    }
}
