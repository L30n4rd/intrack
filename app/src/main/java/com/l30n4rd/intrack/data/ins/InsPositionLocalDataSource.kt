package com.l30n4rd.intrack.data.ins

import com.l30n4rd.intrack.model.AccelerationSample
import com.l30n4rd.intrack.model.InsPosition
import kotlinx.coroutines.flow.Flow

enum class RecordingOption {
    POSITION_SAMPLES,
    POSITION_UPDATE_RATE,
    ACCELERATION_SAMPLES_RAW,
    ACCELERATION_SAMPLES_FILTERED,
    ACCELERATION_UPDATE_RATE,
    VELOCITY_SAMPLES_RAW,
    VELOCITY_SAMPLES_FILTERED,
    VELOCITY_UPDATE_RATE
}

interface InsPositionLocalDataSource {

    fun observePosition(): Flow<InsPosition>

    fun setRotation(rotationMatrix: FloatArray)

    fun resetRotationOffset()

    fun resetPositionAndVelocity()

    fun resetLinearAccelerationOffset()

    fun startRecording(option: RecordingOption)

    fun stopRecording(option: RecordingOption): List<AccelerationSample>
    
    fun setPosition(position: FloatArray)
}
