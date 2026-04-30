package com.example.liber.feature.reader

/** Screen-pixel rectangle for a single highlight line segment, with its ARGB color. */
data class HighlightRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val color: Int,
    val annotationId: Long,
)

/** Screen-pixel position for a draggable selection handle. */
data class SelectionAnchor(val x: Float, val y: Float)
