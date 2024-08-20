package com.l30n4rd.intrack.data.ins.impl

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.SystemClock
import com.l30n4rd.intrack.data.ins.InsPositionLocalDataSource
import com.l30n4rd.intrack.data.ins.RecordingOption
import com.l30n4rd.intrack.model.AccelerationSample
import com.l30n4rd.intrack.model.InsPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class NoFilterInsPositionLocalDataSourceImpl @Inject constructor (
    @ApplicationContext private val context: Context
): InsPositionLocalDataSource {
    private var isRecording: Boolean = false
    private val recordedData: MutableList<AccelerationSample> = mutableListOf()

    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val gameRotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val linearAccelerationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val gameRotationListener = GameRotationListener()
    private val linearAccelerationListener = LinearAccelerationListener()

    private var gameRotationVector = FloatArray(3) { 0.0f }
    private var localLinearVelocityVector = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f) // We need this vector in homogeneous coordinate representation

    private var gameRotationLastTimestampNanos: Long = 0
    private var linearAccelerationLastTimestampNanos: Long = 0
    private var positionUpdateLastTimestampNanos: Long = 0

    private var linearAccelerationOffsetVector = FloatArray(3) { 0.0f}
    private var resetLinearAccelerationOffsetFlag = true

    private val insPositionFlow = MutableStateFlow(InsPosition(transformationMatrix = FloatArray(16) { 0.0f }))
    private var currentPositionMatrix: FloatArray = floatArrayOf(
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )

    private var rotationOffsetMatrix: FloatArray = floatArrayOf(
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    )

    init {
        // Register GameRotationVector sensor
        sensorManager.registerListener(
            gameRotationListener,
            gameRotationVectorSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        // Register LinearAcceleration sensor
        sensorManager.registerListener(
            linearAccelerationListener,
            linearAccelerationSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    private fun updatePosition() {
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

        // Convert the devices local velocity vector to a global one
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
        insPositionFlow.value = InsPosition(currentPositionMatrix.clone())
    }

    override fun observePosition(): Flow<InsPosition> {
        return insPositionFlow
    }

    override fun setRotation(rotationMatrix: FloatArray) {
        rotationOffsetMatrix = rotationMatrix.clone()
        updatePosition()
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
        localLinearVelocityVector = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
        updatePosition()
    }

    override fun resetLinearAccelerationOffset() {
        resetLinearAccelerationOffsetFlag = true
    }

    override fun startRecording(option: RecordingOption) {
        isRecording = true
        recordedData.clear()
    }

    override fun stopRecording(option: RecordingOption): List<AccelerationSample> {
        isRecording = false
        return recordedData.toList()
    }

    override fun setPosition(position: FloatArray) {
        TODO("Not yet implemented")
    }

    // GameRotationVectorListener
    inner class GameRotationListener : SensorEventListener {

        init {
            gameRotationLastTimestampNanos = SystemClock.elapsedRealtimeNanos()
        }
        override fun onSensorChanged(event: SensorEvent?) {
            gameRotationVector = event!!.values.clone()
            gameRotationLastTimestampNanos = event.timestamp
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    // LinearAccelerationListener
    inner class LinearAccelerationListener : SensorEventListener {

        private var previousLinearAccelerationVector = FloatArray(3) { 0.0f }

        init {
            linearAccelerationLastTimestampNanos = SystemClock.elapsedRealtimeNanos()
        }

        override fun onSensorChanged(event: SensorEvent?) {
            val newLinearAccelerationVector = event!!.values.clone()

//            // Set the current values as offset if the flag was set. The flag should be set to true, when the sensor is standing still
//            if (resetLinearAccelerationOffsetFlag) {
//                linearAccelerationOffsetVector = newLinearAccelerationVector
//                resetLinearAccelerationOffsetFlag = false
//            }
//
//            // Apply the offset
//            for (i in 0..2) {
//                newLinearAccelerationVector[i] -= linearAccelerationOffsetVector[i]
//            }
//
//            val dT = (event.timestamp - linearAccelerationLastTimestampNanos) * NS2S
//
//            for (i in 0..2) {
//                localLinearVelocityVector[i] += (previousLinearAccelerationVector[i] + newLinearAccelerationVector[i]) / 2.0f * dT
//            }
//            previousLinearAccelerationVector = newLinearAccelerationVector
//
//            linearAccelerationLastTimestampNanos = event.timestamp

            if (isRecording) {
                val accelerationSample = AccelerationSample(event.timestamp, newLinearAccelerationVector)
                recordedData.add(accelerationSample)
            }

//            updatePosition()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    companion object {
        private const val TAG = "NoFilterInsPositionLocalDataSource"
        private const val NS2S = 1.0f / 1000000000.0f
    }

}

//@Module
//@InstallIn(ActivityComponent::class)
//abstract class InsModule {
//
//    @Binds
//    abstract fun bindInsPositionLocalDataSource(
//        noFilterInsPositionLocalDataSourceImpl: NoFilterInsPositionLocalDataSourceImpl
//    ): InsPositionLocalDataSource
//}
