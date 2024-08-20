package com.l30n4rd.intrack.data

import com.l30n4rd.intrack.model.ApriltagDetection

/**
 * Data class that represents the current UI state in terms of [locationX], [locationY]
 * and [azimuth].
 */
data class PositionUiState(
    val locationX: Float = 0.0f,
    val locationY: Float = 0.0f,
    val azimuth: Float = 0.0f,

    val transformationMatrix: FloatArray = FloatArray(16),
    val detectedTags: List<ApriltagDetection> = emptyList()
)
