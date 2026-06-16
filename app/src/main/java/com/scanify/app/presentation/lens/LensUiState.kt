package com.scanify.app.presentation.lens

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

data class LensTextElement(
    val id: String,
    val text: String,
    val rawBoundingBox: Rect,
    val cornerPoints: List<Point>
)

sealed interface LensUiState {
    data object Idle : LensUiState
    data object Analyzing : LensUiState
    data class Success(
        val elements: List<LensTextElement>,
        val imageSize: Size
    ) : LensUiState
    data class Error(val message: String) : LensUiState
}

object CoordinateMappingUtils {
    class CoordinateMapper(private val matrix: Matrix) {
        fun mapRect(rect: Rect): androidx.compose.ui.geometry.Rect {
            val pts = floatArrayOf(
                rect.left.toFloat(), rect.top.toFloat(),
                rect.right.toFloat(), rect.bottom.toFloat()
            )
            matrix.mapPoints(pts)
            return androidx.compose.ui.geometry.Rect(
                left = pts[0], top = pts[1],
                right = pts[2], bottom = pts[3]
            )
        }

        fun buildPerspectivePath(points: List<Point>): Path {
            val path = Path()
            if (points.size == 4) {
                val pts = FloatArray(8)
                for (i in 0..3) {
                    pts[i * 2] = points[i].x.toFloat()
                    pts[i * 2 + 1] = points[i].y.toFloat()
                }
                matrix.mapPoints(pts)
                path.moveTo(pts[0], pts[1])
                path.lineTo(pts[2], pts[3])
                path.lineTo(pts[4], pts[5])
                path.lineTo(pts[6], pts[7])
                path.close()
            }
            return path
        }
    }

    fun calculateMappingMatrix(viewSize: Size, imageSize: Size): CoordinateMapper {
        val matrix = Matrix()
        if (viewSize.width > 0 && viewSize.height > 0 && imageSize.width > 0 && imageSize.height > 0) {
            val scaleX = viewSize.width / imageSize.width
            val scaleY = viewSize.height / imageSize.height
            val scale = minOf(scaleX, scaleY)

            val dx = (viewSize.width - imageSize.width * scale) / 2f
            val dy = (viewSize.height - imageSize.height * scale) / 2f

            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
        }
        return CoordinateMapper(matrix)
    }
}