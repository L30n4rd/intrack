package com.l30n4rd.intrack.data.apriltag

import android.content.Context
import android.opengl.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import com.l30n4rd.intrack.model.ApriltagDetection
import com.l30n4rd.intrack.utils.PositionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject

class AprilTagHandler @Inject constructor (
    @ApplicationContext private val context: Context,
    private val aprilTagRepository: AprilTagRepository
) {
    val imageAnalysis: ImageAnalysis

    init {
        val cameraHelper = AprilTagCameraHelper(
            context = context,
            tagsizeInMeters = 0.06,
            cameraExecutor = Executors.newSingleThreadExecutor(),
            onTagsDetected = ::onTagsDetected
        )
        imageAnalysis = cameraHelper.getImageAnalysis()
    }

    private fun onTagsDetected(tags: List<ApriltagDetection>) {
        val transformedTags = tags.map { transformToLocalCoordinateSystem(it) }

        val sample = PositionHelper.getTransformationMatrixFromTag(transformedTags.first())
        val python = PositionHelper.getPython4x4TransformationString(sample)
        val json = PositionHelper.getJson4x4TransformationString(sample)

        val ids = tags.joinToString { it.id.toString() }
        Log.d(TAG, "Tags detected: $ids")

        // Propagate the detected tags to the repository
        aprilTagRepository.onTagsDetected(transformedTags)
    }

    private fun transformToLocalCoordinateSystem(tag: ApriltagDetection): ApriltagDetection {
        // Apply transformation to tag pose
        val tagMatrix = PositionHelper.getTransformationMatrixFromTag(tag)
        val transformedTagMatrix = FloatArray(16)
        Matrix.multiplyMM(
            transformedTagMatrix, 0,
            tagToGlobalCoordinateSystemTransformation, 0,
            tagMatrix, 0,
        )

        // Create a new ApriltagPose that uses the global coordinate system
        val transformedPose = PositionHelper.getApriltagPoseFromMatrix(transformedTagMatrix)

        // Return a new ApriltagDetection with the transformed pose
        return tag.copy(pose = transformedPose)
    }

    companion object {
        private const val TAG = "AprilTagHandler"

        /**
         * The camera sensor is rotated by 90° about the z-axis in reference to the local coordinate
         * system. To take this into account in the pose estimation of the tag, we rotate the April
         * Tag coordinate system by -90° about the z-axis.
         * Rotate the tag pose in opposite direction to rotate the coordinate system.
         */
        val tagToGlobalCoordinateSystemTransformation = floatArrayOf(
             0.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f,
             0.0f, 0.0f, 1.0f, 0.0f,
             0.0f, 0.0f, 0.0f, 1.0f
        )
    }
}
