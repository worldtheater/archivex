package com.worldtheater.archive.ui.shape

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min

/**
 * A specialized shape for capsule-like elements (pill shapes) that provides a smoother,
 * continuous curvature at the rounded ends compared to a standard semi-circle.
 *
 * This implementation replaces standard circular arcs with a combination of quadratic
 * Bezier curves and smaller arcs to eliminate the curvature discontinuity at the join points.
 *
 * This shape is designed to be used when the desired corner radius is exactly 50%
 * of the smaller dimension (i.e., a full capsule).
 */
class SmoothCapsuleShape : Shape {

    private val controlPointOffset = 0.585786f
    private val roundJointOffset = 0.292893f
    private val smoothFactor = 1.175f

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height
        val minDimension = min(width, height)
        val maxDimension = max(width, height)

        // Base radius is half of the smaller dimension (standard capsule radius)
        val radius = minDimension * 0.5f

        // If dimensions are too small or invalid, fallback to standard path or empty
        if (radius <= 0f) {
            return Outline.Generic(Path())
        }

        val path = Path()

        val isHorizontal = width > height
        val optimizedCornerSize = min(smoothFactor * radius, 0.5f * maxDimension)

        if (isHorizontal) {
            buildHorizontalCapsule(path, 0f, 0f, width, height, radius, optimizedCornerSize)
        } else {
            buildVerticalCapsule(path, 0f, 0f, width, height, radius, optimizedCornerSize)
        }

        path.close()
        return Outline.Generic(path)
    }

    private fun buildHorizontalCapsule(
        path: Path,
        left: Float, top: Float, right: Float, bottom: Float,
        cornerSize: Float, cornerSizeOptimized: Float
    ) {
        val cpOffset = controlPointOffset * cornerSize
        val rCut = roundJointOffset * cornerSize

        // Top edge
        val startPointX = left + cornerSizeOptimized
        val startPointY = top

        // Top-Right Corner
        val trStartX = right - cornerSizeOptimized
        val trStartY = top

        val trCpX = right - cpOffset
        val trCpY = top

        val trEndX = right - rCut
        val trEndY = top + rCut

        // Right Arc Rect
        val rightRectLeft = right - (2.0f * cornerSize)
        val rightRect = Rect(rightRectLeft, top, right, bottom)

        // Bottom-Right Corner
        val brCpX = right - cpOffset
        val brCpY = bottom

        val brEndX = right - cornerSizeOptimized
        val brEndY = bottom

        // Bottom-Left Corner
        val blStartX = left + cornerSizeOptimized
        val blStartY = bottom

        val blCpX = left + cpOffset
        val blCpY = bottom

        val blEndX = left + rCut
        val blEndY = bottom - rCut

        // Left Arc Rect
        val leftRectRight = left + (cornerSize * 2.0f)
        val leftRect = Rect(left, top, leftRectRight, bottom)

        // Top-Left Corner
        val tlCpX = left + cpOffset
        val tlCpY = top

        val tlEndX = left + cornerSizeOptimized
        val tlEndY = top

        // Building the Path
        path.moveTo(startPointX, startPointY)

        // Line to Top-Right Start
        path.lineTo(trStartX, trStartY)

        // Curve to Top-Right End
        path.quadraticTo(trCpX, trCpY, trEndX, trEndY)

        // Arc at Right
        path.arcTo(rightRect, 270f + 45f, 90f, false)
        path.quadraticTo(brCpX, brCpY, brEndX, brEndY)

        // Line to Bottom-Left Start
        path.lineTo(blStartX, blStartY)

        // Curve to Bottom-Left End
        path.quadraticTo(blCpX, blCpY, blEndX, blEndY)

        // Arc at Left
        path.arcTo(leftRect, 135f, 90f, false)

        // Curve to Top-Left End (which effectively closes back to startPoint if we followed logic)
        path.quadraticTo(tlCpX, tlCpY, tlEndX, tlEndY)
    }

    private fun buildVerticalCapsule(
        path: Path,
        left: Float, top: Float, right: Float, bottom: Float,
        cornerSize: Float, cornerSizeOptimized: Float
    ) {
        val cpOffset = controlPointOffset * cornerSize
        val rCut = roundJointOffset * cornerSize

        // Top rect for arc
        val topRectBottom = top + (cornerSize * 2.0f)
        val topRect = Rect(left, top, right, topRectBottom)

        // Bottom rect for arc
        val bottomRectTop = bottom - (2.0f * cornerSize)
        val bottomRect = Rect(left, bottomRectTop, right, bottom)

        // Start Point (Top-Right-ish side)
        val startPointX = right
        val startPointY = top + cornerSizeOptimized

        // Bottom-Right Corner
        val brStartX = right
        val brStartY = bottom - cornerSizeOptimized

        val brCpX = right
        val brCpY = bottom - cpOffset

        val brEndX = right - rCut
        val brEndY = bottom - rCut

        // Bottom-Left Corner
        val blCpX = left
        val blCpY = bottom - cpOffset

        val blEndX = left
        val blEndY = bottom - cornerSizeOptimized

        // Top-Left Corner
        val tlStartX = left
        val tlStartY = top + cornerSizeOptimized

        val tlCpX = left
        val tlCpY = top + cpOffset

        val tlEndX = left + rCut
        val tlEndY = top + rCut

        // Top-Right Corner
        val trCpX = right
        val trCpY = top + cpOffset

        val trEndX = right
        val trEndY = top + cornerSizeOptimized

        path.moveTo(startPointX, startPointY)
        path.lineTo(brStartX, brStartY)

        // Curve BR
        path.quadraticTo(brCpX, brCpY, brEndX, brEndY)

        // Bottom Arc
        path.arcTo(bottomRect, 45f, 90f, false)

        // Curve BL
        path.quadraticTo(blCpX, blCpY, blEndX, blEndY)

        path.lineTo(tlStartX, tlStartY)

        // Curve TL
        path.quadraticTo(tlCpX, tlCpY, tlEndX, tlEndY)

        // Top Arc
        path.arcTo(topRect, 225f, 90f, false)

        // Curve TR
        path.quadraticTo(trCpX, trCpY, trEndX, trEndY)
    }
}
