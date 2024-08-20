package com.l30n4rd.intrack.model

import kotlinx.serialization.Serializable

@Serializable
/**
 * Transformation from the camera optical frame to
 * the April tag frame. The pose refers to the position of the tag within
 * the camera frame.
 * @param t Translation vector 3x1 of doubles.
 * @param r Rotation matrix 3x3 of doubles. The matrix is in row-major order.
 */
data class ApriltagPose(
    val t: List<Double> = listOf(0.0, 0.0, 0.0),
    val r: List<Double> = listOf(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0
    )
)

@Serializable
data class ApriltagDetection(
    /**
     * The decoded ID of the tag.
     */
    val id: Int = -1,
    /**
     * The center of the detection in image pixel coordinates.
     */
    val c: List<Double> = listOf(0.0, 0.0),
    /**
     * The corners of the tag in image pixel coordinates. These always
     * wrap counter-clock wise around the tag.
     */
    val p: List<List<Double>> = listOf(listOf(0.0, 0.0), listOf(0.0, 0.0), listOf(0.0, 0.0), listOf(0.0, 0.0)),
    /**
     * Object-space error of returned pose.
     */
    val err: Double = 0.0,
    /**
     * Estimate pose of the tag.
     */
    val pose: ApriltagPose = ApriltagPose()
)

data class TagWithGlobalPose(
    /**
     * The detected tag.
     */
    var tag: ApriltagDetection = ApriltagDetection(),
    /**
     * The pose of the tag in reference to an arbitrary global reference point.
     * 4x4 column-major transformation matrix.
     */
    var globalPose: FloatArray? = null,
    /**
     * Timestamp for the last global pose update.
     */
    var lastUpdateTimestampNanos: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagWithGlobalPose

        if (tag != other.tag) return false
        if (!globalPose.contentEquals(other.globalPose)) return false
        if (lastUpdateTimestampNanos != other.lastUpdateTimestampNanos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + globalPose.contentHashCode()
        result = 31 * result + lastUpdateTimestampNanos.hashCode()
        return result
    }
}
