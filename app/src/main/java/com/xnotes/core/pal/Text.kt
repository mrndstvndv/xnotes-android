package com.xnotes.core.pal

/**
 * The selectable text typefaces. The core picks one of these abstract faces; the
 * host resolves each to a concrete platform font. [MONO] is the historical default
 * (and what files without a face stored fall back to), so older notes are unchanged.
 */
class FontFace(val id: String) {
    companion object {
        val SANS = FontFace("sans")
        val SERIF = FontFace("serif")
        val MONO = FontFace("mono")
        val HAND = FontFace("hand")

        private val customFaces = java.util.concurrent.ConcurrentHashMap<String, FontFace>()

        fun fromId(id: String?): FontFace {
            if (id == null) return MONO
            return when (id) {
                "sans" -> SANS
                "serif" -> SERIF
                "mono" -> MONO
                "hand" -> HAND
                else -> customFaces.getOrPut(id) { FontFace(id) }
            }
        }

        fun clearCustomFaces() {
            customFaces.clear()
        }

        val entries: List<FontFace>
            get() = listOf(SANS, SERIF, MONO, HAND) + customFaces.values.sortedBy { it.id }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontFace) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = id
}

/**
 * A font request: a point size, an abstract [face] (resolved to a concrete family
 * by the host — spec 01 §12), and a weight. Defaults to monospace so existing
 * call sites and stored notes render exactly as before.
 */
data class FontSpec(val pointSize: Double, val face: FontFace = FontFace.MONO, val bold: Boolean = false)

/** Text layout flags (spec 01 §1 `draw_text`). Text boxes use the defaults. */
data class TextFlags(
    val wordWrap: Boolean = true,
    val alignLeft: Boolean = true,
    val alignTop: Boolean = true,
)
