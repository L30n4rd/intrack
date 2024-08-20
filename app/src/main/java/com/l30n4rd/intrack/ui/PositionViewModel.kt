package com.l30n4rd.intrack.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l30n4rd.intrack.data.PositionRepository
import com.l30n4rd.intrack.data.PositionUiState
import com.l30n4rd.intrack.data.apriltag.AprilTagHandler
import com.l30n4rd.intrack.data.ins.InsRepository
import com.l30n4rd.intrack.utils.FileHelper
import com.l30n4rd.intrack.utils.PositionHelper.Companion.getAzimuthFromRotationMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PositionViewModel @Inject constructor(
    private val insRepository: InsRepository,
    private val aprilTagHandler: AprilTagHandler,
    private val positionRepository: PositionRepository,
    private val fileHelper: FileHelper
) : ViewModel() {

    /**
     * Position state
     */
    private val _uiState = MutableStateFlow(PositionUiState())
    val uiState: StateFlow<PositionUiState> = _uiState.asStateFlow()

    /**
     * Camera
     */
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector
    private var boundPreviewUseCase: UseCase? = null
    private var boundAnalysisUseCase: UseCase? = null

    init {
        viewModelScope.launch {
            positionRepository.position
                .collect { position ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            locationX = position[12],
                            locationY = position[13],
                            azimuth = getAzimuthFromRotationMatrix(position),
                            transformationMatrix = position
                        )
                    }
                }
        }

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
    }

    // Release camera resources when ViewModel is no longer needed
    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        boundAnalysisUseCase = null
        boundPreviewUseCase = null
        context = null
    }

    // Initialize camera
    fun initializeCamera() {
        context?.let {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(it)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    // No need to bind any use cases initially
                } catch (exc: Exception) {
                    Log.e(TAG, "Camera initialization failed", exc)
                }
            }, ContextCompat.getMainExecutor(it))
        }
    }

    fun setContext(context: Context) {
        this.context = context
    }

    fun bindPreviewUseCase(preview: Preview, previewView: PreviewView) {
        if (cameraProvider == null) {
            initializeCamera()
        }
        cameraProvider?.let { provider ->
            try {
                // Unbind any existing preview use case before binding a new one
                provider.unbind(boundPreviewUseCase)

                // Bind preview use case
                camera = provider.bindToLifecycle(
                    context as LifecycleOwner, cameraSelector, preview)

                // Attach the preview to the previewView
                preview.setSurfaceProvider(previewView.surfaceProvider)

                boundPreviewUseCase = preview

            } catch (exc: Exception) {
                Log.e(TAG, "Error adding preview use case", exc)
            }
        }
    }

    private fun bindAnalysisUseCase(imageAnalyzer: ImageAnalysis) {
        if (cameraProvider == null) {
            initializeCamera()
        }
        cameraProvider?.let { provider ->
            try {
                // Unbind any existing analysis use case before binding a new one
                provider.unbind(boundAnalysisUseCase)

                // Bind analysis use case
                camera = provider.bindToLifecycle(
                    context as LifecycleOwner, cameraSelector, imageAnalyzer)

                boundAnalysisUseCase = imageAnalyzer

            } catch (exc: Exception) {
                Log.e(TAG, "Error adding analysis use case", exc)
            }
        }
    }

    fun unbindPreviewUseCase() {
        cameraProvider?.let { provider ->
            try {
                provider.unbind(boundPreviewUseCase)
                boundPreviewUseCase = null
            } catch (exc: Exception) {
                Log.e(TAG, "Error unbinding preview use case", exc)
            }
        }
    }

    private fun unbindAnalysisUseCase() {
        cameraProvider?.let { provider ->
            try {
                provider.unbind(boundAnalysisUseCase)
                boundAnalysisUseCase = null
            } catch (exc: Exception) {
                Log.e(TAG, "Error unbinding analysis use case", exc)
            }
        }
    }

    fun resetRotationOffset() {
        insRepository.resetRotationOffset()
    }

    fun resetPositionAndVelocity() {
        insRepository.resetPositionAndVelocity()
    }

    fun startTagDetection() {
        Log.d(TAG, "startTagDetection")
        bindAnalysisUseCase(aprilTagHandler.imageAnalysis)
    }

    fun stopTagDetection() {
        unbindAnalysisUseCase()
    }

    fun startPositionRecording() {
        positionRepository.startPositionRecording()
    }

    fun startAprilTagRecording() {
        positionRepository.startAprilTagRecording()
    }

    /**
     * Show a share sheet for recorded data
     *
     * @param context Android context to show the share sheet in
     */
    fun stopPositionRecordingAndShare(context: Context) {
        val fileName = "positionSamples.json"
        val positionData = positionRepository.stopPositionRecording()
        val savedFile = fileHelper.saveStringToFile(positionData, fileName)

        // Share the saved file using Android Sharesheet
        val contentUri: Uri =
            FileProvider.getUriForFile(context, "com.l30n4rd.intrack.fileprovider", savedFile)
        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                "Share recorded position data"
            )
        )
    }

    private fun notifyError(exception: Throwable) {
        Log.e(TAG, "An error occurred: ${exception.message}", exception)
    }

    fun stopAprilTagRecording(context: Context) {
        val fileName = "tagSamples.json"
        val tagData = positionRepository.stopAprilTagRecording()
        val savedFile = fileHelper.saveStringToFile(tagData, fileName)

        // Share the saved file using Android Sharesheet
        val contentUri: Uri =
            FileProvider.getUriForFile(context, "com.l30n4rd.intrack.fileprovider", savedFile)
        val intent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                "Share recorded tag data"
            )
        )
    }

    fun startStepWidthCalculation() {
        positionRepository.startStepWidthCalculation()
    }

    fun stopStepWidthCalculation() {
        positionRepository.stopStepWidthCalculation()
    }

    fun startGroundTruthRecording() {
        positionRepository.startGroundTruthRecording()
    }

    fun addNextGroundTruthPoint() {
        positionRepository.addNextGroundTruthPoint()
    }

    fun addGroundTruthPoint() {
        positionRepository.addNextGroundTruthPoint()
    }

    companion object {
        private const val TAG = "PositionViewModel"
    }
}
