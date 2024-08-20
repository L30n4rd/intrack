package com.l30n4rd.intrack.data.ins.impl

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.HardwareBuffer
import android.hardware.Sensor
import android.hardware.SensorDirectChannel
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import com.l30n4rd.intrack.data.ins.InsPositionLocalDataSource
import com.l30n4rd.intrack.data.ins.RecordingOption
import com.l30n4rd.intrack.model.AccelerationSample
import com.l30n4rd.intrack.model.InsPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AccelerationDirectChannelInsPositionLocalDataSourceImpl @Inject constructor (
    @ApplicationContext private val context: Context
): InsPositionLocalDataSource {
    var stepWidth = 0.0f // Just to make it backwards compatible. This value is not used here.

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private val sensorManager: SensorManager
    private val accelerationSensor: Sensor?
    private val gameRotationVectorSensor: Sensor?

    private val gameRotationListener: GameRotationListener

    private val insPositionFlow = MutableStateFlow(InsPosition(transformationMatrix = FloatArray(16) { 0.0f }))
    private var currentPositionMatrix: FloatArray
    private var rotationOffsetMatrix: FloatArray
    private var gameRotationVector: FloatArray
    private var positionUpdateLastTimestampNanos: Long


    // SensorDirectChannel and HardwareBuffer
    private lateinit var accelerationInsJNI: AccelerationInsJNI
    private lateinit var sensorDirectChannel: SensorDirectChannel
    private lateinit var hardwareBuffer: HardwareBuffer

    init {
        TAG = this::class.java.simpleName

        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager

        // Position
        positionUpdateLastTimestampNanos = 0
        currentPositionMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )

        // Rotation
        gameRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        gameRotationVector = FloatArray(3) { 0.0f }
        gameRotationListener = GameRotationListener()
        rotationOffsetMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
        // Register GameRotationVector sensor
        sensorManager.registerListener(
            gameRotationListener,
            gameRotationVectorSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        // Acceleration
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val accelerationInsJNItemp = initializeDirectChannel()
        if (accelerationInsJNItemp != null){
            accelerationInsJNI = accelerationInsJNItemp
        } else {
            Log.d(TAG, "Direct channel could not be initialized")
        }

        // Observe the current velocity and update the position
        scope.launch {
            accelerationInsJNI.observeVelocity().collect { velocity ->
                updatePosition(floatArrayOf(velocity[0], velocity[1], velocity[2], 1.0f))
            }
        }
    }

    private fun initializeDirectChannel(): AccelerationInsJNI? {
        val supportsHardwareBuffer = accelerationSensor?.isDirectChannelTypeSupported(
            SensorDirectChannel.TYPE_HARDWARE_BUFFER
        )
        if (supportsHardwareBuffer != true) {
            Log.d(TAG, "The acceleration sensor does not support hardware buffer")
            return null
        }

        val accelerationInsJNI = AccelerationInsJNI()

        // Allocate a HardwareBuffer to hold the sensor data
        val bufferSize = 104 // Size of the sensor event structure in bytes
        hardwareBuffer = HardwareBuffer.create(
            bufferSize,
            1,
            HardwareBuffer.BLOB,
            1,
            HardwareBuffer.USAGE_SENSOR_DIRECT_DATA
        )

        // Create a SensorDirectChannel and configure it
        sensorDirectChannel = sensorManager.createDirectChannel(hardwareBuffer)
        sensorDirectChannel.configure(
            accelerationSensor,
            SensorDirectChannel.RATE_VERY_FAST
        )

        // This starts a new thread that processes the sensor values
        accelerationInsJNI.startAccelerationProcessing(hardwareBuffer)

        return accelerationInsJNI
    }

    private fun updatePosition(localLinearVelocityVector: FloatArray) {
        val dTNanos = (SystemClock.elapsedRealtimeNanos() - positionUpdateLastTimestampNanos)
        val dT = dTNanos * NS2S

        // Calculate the rotation matrix from the gameRotationVector
        val tmpRotationMatrix = FloatArray(16) { 0.0f }
        val rotationMatrix = FloatArray(16) { 0.0f }
        SensorManager.getRotationMatrixFromVector(
            tmpRotationMatrix,
            gameRotationVector
        )
        // Transpose the rotation matrix to follow the OpenGL ES format
        Matrix.transposeM(rotationMatrix, 0, tmpRotationMatrix, 0)

        // Apply the rotation offset
        Matrix.multiplyMM(rotationMatrix, 0, rotationMatrix, 0, rotationOffsetMatrix, 0)

        // Transform the devices local velocity vector to a global one
        val worldVelocity = FloatArray(4)
        Matrix.multiplyMV(worldVelocity, 0, rotationMatrix, 0, localLinearVelocityVector, 0)

        // Integrate the velocity to get the new position
        for (i in 0..2) {
            currentPositionMatrix[i + 12] += worldVelocity[i] * dT
        }

        // Copy the 3x3 rotation part from the rotation matrix to the rotation part of the position matrix
        for (column in 0..2) {
            for (row in 0..2) {
                currentPositionMatrix[column * 4 + row] = rotationMatrix[column * 4 + row]
            }
        }

        positionUpdateLastTimestampNanos = SystemClock.elapsedRealtimeNanos()
        insPositionFlow.value = InsPosition(
            currentPositionMatrix.clone(),
            timestamp = SystemClock.elapsedRealtimeNanos()
        )
    }

    // GameRotationVectorListener
    inner class GameRotationListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            gameRotationVector = event!!.values.clone()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    override fun startRecording(option: RecordingOption) {
        AccelerationInsJNI.startRecording()
    }

    override fun stopRecording(option: RecordingOption): List<AccelerationSample> {
        AccelerationInsJNI.stopRecording()
        return AccelerationInsJNI.getRecordedData()
    }

    override fun setPosition(position: FloatArray) {
        TODO("Not yet implemented")
    }


    override fun observePosition(): StateFlow<InsPosition> {
        return insPositionFlow.asStateFlow()
    }

    override fun setRotation(rotationMatrix: FloatArray) {
        rotationOffsetMatrix = rotationMatrix.clone()
    }

    override fun resetRotationOffset() {
        // Use the current rotation from the sensor to calculate the rotation offset in opposite direction
        val tmpRotationMatrix = FloatArray(16) { 0.0f }
        val rotationMatrix = FloatArray(16) { 0.0f }
        val invertedRotationMatrix = FloatArray(16) { 0.0f }
        SensorManager.getRotationMatrixFromVector(
            tmpRotationMatrix,
            gameRotationVector
        )
        // Transpose the rotation matrix to follow the OpenGL ES format
        Matrix.transposeM(rotationMatrix, 0, tmpRotationMatrix, 0)

        Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0)
        setRotation(invertedRotationMatrix)
    }

    override fun resetPositionAndVelocity() {
        // Reset the position and linear velocity vector
        currentPositionMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
        // Reset of velocity not implemented
    }

    override fun resetLinearAccelerationOffset() {}

    fun observeStepDetection(): StateFlow<Int> {
        val stepDetectionFlow: MutableStateFlow<Int> = MutableStateFlow(0)
        return stepDetectionFlow.asStateFlow()
    }

    companion object {
        private var TAG = "InsPositionLocalDataSource"
        private const val NS2S = 1.0f / 1000000000.0f
    }

}
