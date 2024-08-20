package com.l30n4rd.intrack.model

import kotlinx.serialization.Serializable

@Serializable
data class PositionSample(
    val runnerId: Int = 0,
    val timestamp: Long,
    val position: FloatArray,
    val groundTruth: FloatArray? = null,
    val error: Double? = null
)

@Serializable
data class AprilTagSample(
    val tagId: Int,
    val timestamp: Long,
    val globalPose: FloatArray
)
