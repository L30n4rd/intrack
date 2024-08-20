package com.l30n4rd.intrack.data.ins.impl

import android.hardware.HardwareBuffer
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.l30n4rd.intrack.model.AccelerationSample

// Wrapper for native library

class AccelerationInsJNI {
    private val mutableVelocityFlow = MutableStateFlow(FloatArray(3) {0.0f})

    // Create a handler thread for running native code
    private val handlerThread = HandlerThread("AccelerationProcessingThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    fun startAccelerationProcessing(hardwareBuffer: HardwareBuffer): Boolean {
        // Run the `processAccelerationData` function on a background thread
        return try {
            handler.post {
                processAccelerationData(hardwareBuffer)
            }
            true // Return true to indicate successful start
        } catch (e: Exception) {
            // Handle any exceptions
            false // Return false to indicate failure
        }
    }

    fun observeVelocity(): Flow<FloatArray> {
        return mutableVelocityFlow.asStateFlow()
    }

    private fun updateVelocity(x: Float, y: Float, z: Float) {
        val velocity = floatArrayOf(x, y, z)
        mutableVelocityFlow.value = velocity
    }

    private external fun processAccelerationData(hardwareBuffer: HardwareBuffer)

    companion object {

        @JvmStatic
        external fun stopAccelerationProcessing()

        @JvmStatic
        external fun startRecording()

        @JvmStatic
        external fun stopRecording()

        @JvmStatic
        external fun getRecordedData(): List<AccelerationSample>

        init {
            System.loadLibrary("intrack")
        }
    }
}
