package com.l30n4rd.intrack.ui.ins

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.l30n4rd.intrack.utils.PositionHelper.Companion.getAzimuthFromRotationMatrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CoordinateSystem(
    modifier: Modifier,
    position: FloatArray,
    scale: Float = -1.0f
) {
    var dynamicScale = scale
    if (scale <= 0.0f) {
        val locationX = position[12]
        val locationY = position[13]
        val distanceFromOrigin = sqrt(locationX * locationX + locationY * locationY)
        dynamicScale = calculateNearestScale(distanceFromOrigin)
    }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
    ) {
        drawCoordinateSystem(dynamicScale)
        drawCurrentPosition(position, dynamicScale)
    }
}

private fun calculateNearestScale(distance: Float): Float {
    if (distance <= 0.5f) {
        return 0.5f // Minimum scale
    }

    // Determine the order of magnitude of the distance
    val base = 2.0
    val magnitude = base.pow(kotlin.math.ceil(kotlin.math.log(distance.toDouble(), base)).toInt())

    // Determine the significant digit
    val significantDigit = distance / magnitude

    // Round the significant digit to the nearest valid scale factor
    val roundedSignificantDigit = when {
        significantDigit <= 1.5 -> 1f
        significantDigit <= 3 -> 2f
        significantDigit <= 7 -> 5f
        else -> 10f
    }

    // Calculate the nearest scale based on the rounded significant digit and magnitude
    return roundedSignificantDigit * magnitude.toFloat()
}

private fun DrawScope.drawCoordinateSystem(scale: Float = 2f) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    // Draw x-axis
    drawLine(
        start = Offset(0f, canvasHeight / 2),
        end = Offset(canvasWidth, canvasHeight / 2),
        color = Color.Green,
        strokeWidth = 2f,
        alpha = 0.7f
    )

    // Draw y-axis
    drawLine(
        start = Offset(canvasWidth / 2, 0f),
        end = Offset(canvasWidth / 2, canvasHeight),
        color = Color.Green,
        strokeWidth = 2f,
        alpha = 0.7f
    )

    // Draw labels
    val labelMargin = 8f
    val paint = Paint().apply {
        color = Color.Green.toArgb()
        textSize = 16.dp.toPx()
    }
    val labelWidth = paint.measureText("+${scale} m")
    val labelHeight = 16.dp.toPx()
    drawIntoCanvas { canvas ->
        // Draw x-axis labels
        canvas.nativeCanvas.drawText("+${scale} m", canvasWidth - labelWidth - labelMargin, canvasHeight / 2 + labelHeight + labelMargin, paint)
        canvas.nativeCanvas.drawText("-${scale} m", labelMargin, canvasHeight / 2 + labelHeight + labelMargin, paint)

        // Draw y-axis labels
        canvas.nativeCanvas.drawText("+${scale} m", canvasWidth / 2 - labelMargin - labelWidth, labelHeight + labelMargin, paint)
        canvas.nativeCanvas.drawText("-${scale} m", canvasWidth / 2 - labelMargin - labelWidth, canvasHeight - labelMargin, paint)
    }
}

private fun DrawScope.drawCurrentPosition(transformationMatrix: FloatArray, scale: Float) {
    require(scale > 0) { "Scale must be greater than 0" }

    // Extract translation components from the transformation matrix
    val xTranslation = transformationMatrix[12]
    val yTranslation = transformationMatrix[13]

    // Calculate the canvas coordinates based on the translation
    val circleX = size.width / 2 + xTranslation * (size.width / (scale * 2))
    val circleY = size.height / 2 - yTranslation * (size.height / (scale * 2)) // Invert y-axis as canvas y-axis goes downwards

    // Draw a circle to represent the current position
    drawCircle(
        color = Color.Red,
        radius = 16f,
        center = Offset(circleX, circleY)
    )

    // Calculate line coordinates based on azimuth
    val azimuth = getAzimuthFromRotationMatrix(transformationMatrix) + (PI/2) // Our azimuth is 0 when we facing to the y-axis
    val lineLength = 32f // Length of the line to represent orientation
    val lineEndX = circleX + lineLength * cos(azimuth).toFloat()
    val lineEndY = circleY - lineLength * sin(azimuth).toFloat() // Invert y-axis as canvas y-axis goes downwards

    // Draw a line to represent the orientation
    drawLine(
        color = Color.Cyan,
        start = Offset(circleX, circleY),
        end = Offset(lineEndX, lineEndY),
        strokeWidth = 4f
    )
}
