package com.l30n4rd.intrack.data.apriltag

import java.nio.ByteBuffer

// Wrapper for native library

class AprilTagProcessorJNI {

    companion object {
        init {
            System.loadLibrary("intrack")
        }

        // External native method declaration
        @JvmStatic
        external fun detectTags(
            imageBuffer: ByteBuffer,
            width: Int,
            height: Int,
            tagsizeInMeters: Double,
            focalLengthInPixelsX: Double,
            focalLengthInPixelsY: Double,
            focalCenterInPixelsX: Double,
            focalCenterInPixelsY: Double
        ): String
    }
}
