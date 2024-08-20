package com.l30n4rd.intrack.data

import android.content.Context
import android.opengl.Matrix
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.l30n4rd.intrack.data.apriltag.AprilTagRepository
import com.l30n4rd.intrack.data.ins.InsRepository
import com.l30n4rd.intrack.model.AprilTagSample
import com.l30n4rd.intrack.model.ApriltagDetection
import com.l30n4rd.intrack.model.InsPosition
import com.l30n4rd.intrack.model.PositionSample
import com.l30n4rd.intrack.model.TagWithGlobalPose
import com.l30n4rd.intrack.utils.PositionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.sqrt

class PositionRepository @Inject constructor(
    private val insRepository: InsRepository,
    private val aprilTagRepository: AprilTagRepository,
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator?

    private var isCurrentPositionKnown = false
    private var isRecordingPosition: Boolean = false
    private var isRecordingAprilTags: Boolean = false
    private val recordedPositionData: MutableList<PositionSample> = mutableListOf()
    private val recordedAprilTagData: MutableList<AprilTagSample> = mutableListOf()

    private val _position: MutableStateFlow<FloatArray> = MutableStateFlow(floatArrayOf(
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    ))
    /**
     * Current position as column-major transformation matrix.
     */
    val position: StateFlow<FloatArray> = _position.asStateFlow()

    private var isStepWidthCalculationRunning = false
    private var stepWidthCalculationPositionStart = _position.value
    private var stepWidthCalculationPositionEnd = _position.value
    private var stepCounter = 0

    private var currentGroundTruthPointIndex = 0
    private val groundTruthPoints: List<FloatArray> = listOf(
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            43.1f, 11.6f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            41.1f, 12.4f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            37.1f, 14.4f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            35.2f, 15.4f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            33.1f, 16.8f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            34.1f, 18.2f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            37.6f, 24.3f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            28.7f, 30.1f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            24.3f, 25.1f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            23.1f, 23.9f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            19.0f, 29.0f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            19.7f, 31.6f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            20.7f, 32.5f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            22.4f, 33.2f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            24.4f, 33.3f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            28.7f, 30.1f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            24.3f, 25.1f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            23.1f, 23.9f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            33.1f, 16.8f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            34.1f, 18.2f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            37.6f, 24.3f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            40.4f, 23.1f, 0.0f, 1.0f
        ),
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            44.7f, 22.0f, 0.0f, 1.0f
        )
    )

    init {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        scope.launch {
            insRepository.position.collect { position ->
                onInsPositionUpdated(position)
            }
        }
        scope.launch {
            insRepository.stepDetection.collect {
                stepCounter += 1
            }
        }
        scope.launch {
            aprilTagRepository.detectedTags.collect { tags ->
                onTagsDetected(tags)
            }
        }
    }

    private fun onInsPositionUpdated(position: InsPosition) {
        _position.value = position.transformationMatrix
        if (isRecordingPosition) {
            val positionSample = PositionSample(
                runnerId = 0,
                timestamp = position.timestamp,
                position = position.transformationMatrix
            )
            recordedPositionData.add(positionSample)
        }
    }

    private fun onTagsDetected(tags: List<ApriltagDetection>) {
        val mutableTagList = tags.toMutableList()
        val anchorTag = aprilTagRepository.getAnchorTag()

        // If anchor tag is in the list, move it to the front so it gets processed first
        val anchorTagIndex = mutableTagList.indexOfFirst { it.id == anchorTag.tag.id }
        if (anchorTagIndex != -1) {
            val detectedAnchorTag = mutableTagList.removeAt(anchorTagIndex)
            mutableTagList.add(0, detectedAnchorTag)
        }

        // Process tags
        for (tag in mutableTagList) {
            // Add the tag to the recording
            if (isRecordingAprilTags) {
                aprilTagRepository.getTag(tag.id)?.let {
                    recordAprilTagSample(it)
                }
            }

            // Update position if tag is the anchor tag
            if (tag.id == anchorTag.tag.id) {
                // Update the current relative pose of the tag first
                val tagWithAnchorPose = anchorTag.copy(tag = tag)

                val anchorRelativeMatrix = PositionHelper.getTransformationMatrixFromTag(tagWithAnchorPose.tag)
                val anchorStr = PositionHelper.getPython4x4TransformationString(tagWithAnchorPose.globalPose!!)

                updatePositionByTag(tagWithAnchorPose)
                continue
            }

            // Update position if tag has a known global pose
            val tagWithGlobalPose = aprilTagRepository.getTag(tag.id)
            if (tagWithGlobalPose?.globalPose != null) {
                // Update the current relative pose of the tag first
                val updatedTagWithGlobalPose = tagWithGlobalPose.copy(tag = tag)
                aprilTagRepository.addOrUpdateTag(updatedTagWithGlobalPose)
                updatePositionByTag(updatedTagWithGlobalPose)
                continue
            }

            // Set global pose of tag if current position is known
            if (isCurrentPositionKnown) {
                setGlobalPoseOfTag(_position.value, tag)
            }
        }
    }

    /**
     * This function updates the current position by using the global tag position
     * and the relative position to the camera.
     *
     * @param tag Tag with a known global pose and a relative pose to the camera.
     * @throws IllegalArgumentException if the tag has no global pose.
     */
    private fun updatePositionByTag(tag: TagWithGlobalPose) {
        require(tag.globalPose != null) { "The provided tag must have a global pose" }

        // Update position only if the error of the pose estimation is not to big
        if (tag.tag.err < 1e-9) return

        tag.globalPose?.let {globalTagPose ->
            val relativePoseMatrix = PositionHelper.getTransformationMatrixFromTag(tag.tag)

            // Reverse the relative tag pose so we can get the position by multiplying it with the global tag pose
            val invertedRelativePoseMatrix = FloatArray(16)
            Matrix.invertM(invertedRelativePoseMatrix, 0, relativePoseMatrix, 0)
            Matrix.multiplyMM(
                _position.value, 0,
                globalTagPose, 0,
                invertedRelativePoseMatrix, 0,
            )

            isCurrentPositionKnown = true

            // Update INS position
            insRepository.setPosition(_position.value)

            // Add an position entry with the tag estimate error to investigate jumps in the position
            if (isRecordingPosition) {
                val positionSample = PositionSample(
                    runnerId = 0,
                    timestamp = SystemClock.elapsedRealtimeNanos(),
                    position = _position.value,
                    error = tag.tag.err
                )
                recordedPositionData.add(positionSample)
            }
        }
    }

    /**
     * This function updates the global pose of the tag and stores it to the repository.
     *
     * @param position The current position of the camera.
     * @param tag The detected tag with the relative pose to the camera.
     */
    private fun setGlobalPoseOfTag(position: FloatArray, tag: ApriltagDetection) {
        val relativeTagPose = PositionHelper.getTransformationMatrixFromTag(tag)
        val globalTagPose = FloatArray(16)
        Matrix.multiplyMM(
            globalTagPose, 0,
            position, 0,
            relativeTagPose, 0
        )

        val tagWithGlobalPose = TagWithGlobalPose(tag = tag, globalPose = globalTagPose)
        aprilTagRepository.addOrUpdateTag(tagWithGlobalPose)
    }

    fun startPositionRecording() {
        recordedPositionData.clear()
        isRecordingPosition = true
    }

    fun stopPositionRecording(): String {
        isRecordingPosition = false
        val data = recordedPositionData.toList()
        return Json.encodeToString(data)
    }

    fun startAprilTagRecording() {
        recordedAprilTagData.clear()
        isRecordingAprilTags = true
    }

    fun stopAprilTagRecording(): String {
        isRecordingAprilTags = false
        val data = recordedAprilTagData.toList()
        return Json.encodeToString(data)
    }

    fun recordPositionSampleWithGroundTruth(groundTruth: FloatArray) {
        val sample = PositionSample(
            timestamp = SystemClock.elapsedRealtimeNanos(),
            groundTruth = groundTruth,
            position = _position.value
        )
        recordedPositionData.add(sample)
    }

    private fun recordAprilTagSample(tag: TagWithGlobalPose) {
        tag.globalPose?.let { globalPose ->
            val sample = AprilTagSample(
                timestamp = tag.lastUpdateTimestampNanos,
                tagId = tag.tag.id,
                globalPose = globalPose
            )
            recordedAprilTagData.add(sample)
        }
    }

    fun startStepWidthCalculation() {
        isStepWidthCalculationRunning = true
        stepWidthCalculationPositionStart = _position.value
        stepCounter = 0
    }

    fun stopStepWidthCalculation() {
        isStepWidthCalculationRunning = false
        stepWidthCalculationPositionEnd = _position.value

        // Calculate distance between start and end position
        val diffX = stepWidthCalculationPositionEnd[12] - stepWidthCalculationPositionStart[12]
        val diffY = stepWidthCalculationPositionEnd[13] - stepWidthCalculationPositionStart[13]
        val distance = sqrt(diffX * diffX + diffY * diffY)
        if (stepCounter != 0) {
            val avgStepWidth = distance / stepCounter
            Log.d(TAG, "New average step width: $avgStepWidth m")
            insRepository.setStepWidth(avgStepWidth)
        } else {
            Log.e(TAG, "Could not calculate average step width. Step counter is 0.")
        }
    }

    fun startGroundTruthRecording() {
        if (!isRecordingPosition) {
            startPositionRecording()
        }
        currentGroundTruthPointIndex = 0
    }

    fun addNextGroundTruthPoint() {
        if (currentGroundTruthPointIndex >= groundTruthPoints.size) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            return
        }
        val point = groundTruthPoints[currentGroundTruthPointIndex]
        recordPositionSampleWithGroundTruth(point)
        currentGroundTruthPointIndex += 1
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }

    companion object {
        private var TAG = "PositionRepository"
    }
}
