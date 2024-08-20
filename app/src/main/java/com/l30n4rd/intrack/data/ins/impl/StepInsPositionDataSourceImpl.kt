package com.l30n4rd.intrack.data.ins.impl

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.l30n4rd.intrack.data.ins.InsPositionLocalDataSource
import com.l30n4rd.intrack.data.ins.RecordingOption
import com.l30n4rd.intrack.model.AccelerationSample
import com.l30n4rd.intrack.model.InsPosition
import com.l30n4rd.intrack.utils.PositionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.PI

class StepInsPositionDataSourceImpl @Inject constructor (
    @ApplicationContext private val context: Context
): InsPositionLocalDataSource {
    private val vibrator: Vibrator?

    private val sensorManager: SensorManager
    private val stepDetectorSensor: Sensor?
    private val gameRotationVectorSensor: Sensor?

    private val gameRotationListener: GameRotationListener
    private val stepDetectorListener: StepDetectorListener

    private val insPositionFlow: MutableStateFlow<InsPosition>

    private val _stepDetectionFlow: MutableStateFlow<Int> = MutableStateFlow(0)

    /**
     * Transformation matrix that contains the current positon. The matrix is column-major.
     */
    private var currentPositionMatrix: FloatArray

    /**
     * Rotation matrix that represent the variance between the rotation detected by the sensor
     * and the true rotation of the phone as determined by the InsPositionDataSource.
     * The matrix is column-major.
     */
    private var sensorRotationOffsetMatrix: FloatArray
    private var gameRotationVector: FloatArray
    private var positionUpdateTimestampNanos: Long
    private var azimuthOffset: Float
    var stepWidth: Float

    init {
        TAG = this::class.java.simpleName

        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))

        // Position
        positionUpdateTimestampNanos = 0
        currentPositionMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
        insPositionFlow = MutableStateFlow(InsPosition(
            currentPositionMatrix.clone(),
            timestamp = SystemClock.elapsedRealtimeNanos()
        ))

        // Initialize the sensor rotation offset with 90° about the z-axis because the sensor starts with -90°
        sensorRotationOffsetMatrix = floatArrayOf(
             0.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, 0.0f,
             0.0f, 0.0f, 1.0f, 0.0f,
             0.0f, 0.0f, 0.0f, 1.0f
        )
        gameRotationVector = floatArrayOf(0f, 0f, 0f)
        gameRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        gameRotationListener = GameRotationListener()
        azimuthOffset = 0.0f
        // Register GameRotationVector sensor
        sensorManager.registerListener(
            gameRotationListener,
            gameRotationVectorSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        // StepDetector
        stepWidth = 0.72f // Assumptive step width
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepDetectorListener = StepDetectorListener()
        // Register step detector sensor
        sensorManager.registerListener(
            stepDetectorListener,
            stepDetectorSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    private fun emitCurrentPosition() {
        insPositionFlow.value = InsPosition(
            currentPositionMatrix.clone(),
            timestamp = SystemClock.elapsedRealtimeNanos()
        )
    }

    private fun makeOneStep(stepWidth: Float) {
        // Descibes a step in y-axis direction of the global coordinate system
        val stepVector = floatArrayOf(0.0f, stepWidth, 0.0f, 1.0f)

        // Extract the direction in which we are heading on the xy-plane
        val azimuth = PositionHelper.getAzimuthFromRotationMatrix(currentPositionMatrix)
        val zRotationMatrix = PositionHelper.getRotationMatrixAboutZAxis(azimuth)

        // Apply the rotation around the z-axis to the step vector
        Matrix.multiplyMV(
            stepVector, 0,
            zRotationMatrix, 0,
            stepVector, 0
        )

        // Update the location
        for (i in 0..2) {
            currentPositionMatrix[i + 12] += stepVector[i]
        }

        positionUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
        emitCurrentPosition()
    }

    /**
     * Copy the 3x3 rotation part from the transformation matrix to the rotation part of the position matrix.
     * @param transformationMatrix 4x4 column-major matrix containing the rotation.
     */
    private fun setRotationOfCurrentPosition(transformationMatrix: FloatArray) {
        for (column in 0..2) {
            for (row in 0..2) {
                currentPositionMatrix[column * 4 + row] = transformationMatrix[column * 4 + row]
            }
        }
    }

    /**
     * Copy the 3x1 translation part from the transformation matrix to the translation part of the position matrix.
     * @param transformationMatrix 4x4 column-major matrix containing the translation.
     */
    private fun setTranslationOfCurrentPosition(transformationMatrix: FloatArray) {
        for (i in 0..2) {
            currentPositionMatrix[i + 12] = transformationMatrix[i + 12]
        }
    }

    // GameRotationVectorListener
    inner class GameRotationListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            gameRotationVector = event!!.values.clone()

            /*
             * Calculate the current orientation.
             * We apply the offset on the azimuth only (rotation about the z-axis), but keep the xy-plane
             * as it is measured by the sensor because the Earth's surface is the xy-plane in our
             * global coordinate system, and it's unlikely that you want to tilt it.
             * Bonus tip: If you find yourself wanting to tilt the Earth, please consult a physicist or
             * an angry mob of geologists first.
             */
            val sensorRotationMatrix = PositionHelper.getRotationMatrixFromRotationVector(gameRotationVector)

            val azimuthOffset = PositionHelper.getAzimuthFromRotationMatrix(sensorRotationOffsetMatrix)
            val azimuthOffsetInDegrees = (azimuthOffset * RAD_2_DEGREES).toFloat()

            // Apply the azimuth offset
            val rotationMatrix = sensorRotationMatrix.clone()
            Matrix.rotateM(rotationMatrix, 0, azimuthOffsetInDegrees, 0.0f, 0.0f, 1.0f)

            // Update the rotation
            setRotationOfCurrentPosition(rotationMatrix)
            emitCurrentPosition()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    // StepDetectorListener
    inner class StepDetectorListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // Generate a haptic feedback
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            makeOneStep(stepWidth)
            _stepDetectionFlow.value += 1
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
            Log.d(TAG, "onAccuracyChanged")
        }
    }

    override fun setPosition(position: FloatArray) {
        setRotation(position)
        setTranslationOfCurrentPosition(position)
        emitCurrentPosition()
    }

    override fun setRotation(matrix: FloatArray) {
        // Clean the rotation matrix
        val rotationMatrix = matrix.clone()
        rotationMatrix[12] = 0.0f
        rotationMatrix[13] = 0.0f
        rotationMatrix[14] = 0.0f
        rotationMatrix[15] = 1.0f

        /*
         * Update the sensor rotation offset by multiplying the inverse of the sensor rotation by
         * the rotation matrix which should be the actual rotation.
         * After this calculation, the sensor rotation offset contains the rotation that needs to be
         * applyed to the sensor rotation to get to the passed rotation matrix.
         */
        val sensorRotationMatrix = PositionHelper.getRotationMatrixFromRotationVector(gameRotationVector)
        val invertedSensorRotationMatrix = FloatArray(16)
        Matrix.transposeM(invertedSensorRotationMatrix, 0, sensorRotationMatrix, 0)
        Matrix.multiplyMM(
            sensorRotationOffsetMatrix, 0,
            invertedSensorRotationMatrix, 0,
            rotationMatrix, 0,
        )
    }

    override fun resetPositionAndVelocity() {
        // Reset the position
        setTranslationOfCurrentPosition(floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        ))
    }

    override fun observePosition(): StateFlow<InsPosition> {
        return insPositionFlow.asStateFlow()
    }

    fun observeStepDetection(): StateFlow<Int> {
        return _stepDetectionFlow.asStateFlow()
    }

    override fun startRecording(option: RecordingOption) {
        TODO("Not implemented")
    }

    override fun stopRecording(option: RecordingOption): List<AccelerationSample> {
        TODO("Not implemented")
    }

    override fun resetRotationOffset() {
        TODO("Not implemented because the offset is now handled exclusive by this class")
    }

    override fun resetLinearAccelerationOffset() {
        TODO("Not implemented")
    }

    companion object {
        private var TAG = "InsPositionLocalDataSource"
        private const val RAD_2_DEGREES = 180.0 / PI
    }

}
