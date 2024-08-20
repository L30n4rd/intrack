package com.l30n4rd.intrack.data.ins

import com.l30n4rd.intrack.data.ins.impl.AccelerationDirectChannelInsPositionLocalDataSourceImpl
import com.l30n4rd.intrack.data.ins.impl.RawAccelerationInsPositionLocalDataSourceImpl
import com.l30n4rd.intrack.data.ins.impl.StepInsPositionDataSourceImpl
import com.l30n4rd.intrack.model.InsPosition
import com.l30n4rd.intrack.utils.FileHelper
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsRepository @Inject constructor(
//    private val dataSource: NoFilterInsPositionLocalDataSourceImpl,
//    private val dataSource: RawAccelerationInsPositionLocalDataSourceImpl,
//    private val dataSource: AccelerationDirectChannelInsPositionLocalDataSourceImpl,
    private val dataSource: StepInsPositionDataSourceImpl,
    private val fileHelper: FileHelper
) {
    val position: StateFlow<InsPosition> get() =
        dataSource.observePosition()

    val stepDetection: StateFlow<Int> get() =
        dataSource.observeStepDetection()

    fun resetRotationOffset() {
        dataSource.setRotation(floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        ))
    }

    fun resetPositionAndVelocity() {
        dataSource.resetPositionAndVelocity()
    }

    fun resetLinearAccelerationOffset() {
        dataSource.resetLinearAccelerationOffset()
    }

    fun setRotationOffset(rotationMatrix: FloatArray) {
        dataSource.setRotation(rotationMatrix)
    }

    fun startRecording(option: RecordingOption) {
        dataSource.startRecording(option)
    }

    fun stopRecordingAndSaveToFile(option: RecordingOption): File {
        val fileName = "acceleration.csv"
        val recordedData = dataSource.stopRecording(option)
        val csvData = FileHelper.convertAccelerationDataToCsv(recordedData)
        return fileHelper.saveStringToFile(csvData, fileName)
    }

    fun setPosition(position: FloatArray) {
        dataSource.setPosition(position)
    }

    fun setStepWidth(stepWidth: Float) {
        dataSource.stepWidth = stepWidth
    }
}
