package com.l30n4rd.intrack.data.apriltag

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.runtime.currentRecomposeScope
import com.l30n4rd.intrack.model.ApriltagDetection
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService


/**
 * Helper class for managing camera operations with AprilTags, including starting the camera,
 * handling detected tags, and calculating focal length in pixels.
 *
 * @param context The context used for accessing system services and resources.
 * @param cameraExecutor The executor service used for running camera operations asynchronously.
 * @param tagsizeInMeters The size of AprilTags in meters for accurate distance calculations.
 * @param onTagsDetected Callback function to handle the detected AprilTags.
 */
class AprilTagCameraHelper(
    private val context: Context,
    private val cameraExecutor: ExecutorService,
    private val tagsizeInMeters: Double,
    private val onTagsDetected: (List<ApriltagDetection>) -> Unit
) {

    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraCharacteristics = getCameraCharacteristics(CameraCharacteristics.LENS_FACING_FRONT)

    fun getImageAnalysis(): ImageAnalysis {
        // Select a higher resolution than the default 680x420 pixels
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(
                Size(1920, 1440),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
            .build()


        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, AprilTagAnalyzer())
            }
        return imageAnalysis
    }

    private fun getCameraCharacteristics(lensFacing: Int): CameraCharacteristics? {
        // Get the list of camera IDs
        val cameraIds = cameraManager.cameraIdList

        // Iterate through the available camera IDs
        for (cameraId in cameraIds) {
            // Get the CameraCharacteristics for the current camera ID
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            // Check if the camera has the desired features (e.g., front or back camera)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != null && facing == lensFacing) {
                // Found the desired camera
                Log.d(TAG, "Found camera with ID: $cameraId")
                return characteristics
            }
        }
        return null
    }

    fun calculateFocalLengthInPixels(imageSize: Size): Pair<Double, Double>? {
        if (cameraCharacteristics == null) {
            Log.e(TAG, "Focal length in pixels could not be calculated.")
            return null
        }

        // Get the sensor size in millimeters
        val sensorSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            ?: return null

        // Get focal length in millimeters
        val focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?: return null
        val focalLengthInMillimeters = focalLengths[0]
//        Log.d(TAG, "focalLengthInMillimeters: $focalLengthInMillimeters")

        // Calculate the focal length in pixels
        val focalLengthInPixels = Pair(
            ((focalLengthInMillimeters / sensorSize.width) * imageSize.width).toDouble(),
            ((focalLengthInMillimeters / sensorSize.height) * imageSize.height).toDouble()
        )
//        Log.d(TAG, "focalLengthInPixels: $focalLengthInPixels")

        return focalLengthInPixels
    }

    inner class AprilTagAnalyzer : ImageAnalysis.Analyzer {

        private var analyseRate = 0.0
        private var prevTimestemp = SystemClock.elapsedRealtimeNanos()

        override fun analyze(image: ImageProxy) {
            // Convert the ImageProxy to a ByteBuffer as needed for the JNI code
            val imageBuffer = convertImageProxyToDirectByteBuffer(image)

            // This is true for most smartphone cameras
            val focalCenterInPixels = Pair(image.width / 2.0, image.height / 2.0)

            // TODO: handle exeption
            val focalLengthInPixels = calculateFocalLengthInPixels(Size(image.width, image.height))
                ?: Pair(0.0, 0.0)

            // Call the JNI code with the image data
            val detectedTagsStr = AprilTagProcessorJNI.detectTags(
                imageBuffer,
                image.width,
                image.height,
                tagsizeInMeters,
                focalLengthInPixels.first,
                focalLengthInPixels.second,
                focalCenterInPixels.first,
                focalCenterInPixels.second
            )
            val detectedTags: List<ApriltagDetection> = Json.decodeFromString(detectedTagsStr)
            image.close() // Close the image to release resources

            // Notify listener with detected tags
            if (detectedTags.isNotEmpty()) {
                onTagsDetected(detectedTags)
            }

            val currentTimestamp = SystemClock.elapsedRealtimeNanos()
            analyseRate = 1 / ((currentTimestamp - prevTimestemp) * 1e-9)
            prevTimestemp = currentTimestamp

            Log.d(TAG, "Analyze Rate: $analyseRate")
        }

        private fun convertImageProxyToDirectByteBuffer(image: ImageProxy): ByteBuffer {
            val buffer: ByteBuffer = image.planes[0].buffer
            val directBuffer = ByteBuffer.allocateDirect(buffer.remaining())
            directBuffer.put(buffer)
            return directBuffer
        }
    }

    companion object {
        private const val TAG = "AprilTagCameraHelper"
    }
}
