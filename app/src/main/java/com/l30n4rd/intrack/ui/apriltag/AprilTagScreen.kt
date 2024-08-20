package com.l30n4rd.intrack.ui.apriltag

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED

@Composable
fun AprilTagScreen(
    onInsButtonClicked: () -> Unit,
    onInitializePreview: (Preview, PreviewView) -> Unit,
    onDisposePreview: () -> Unit,
    onStartTagDetectionClicked: () -> Unit,
    onStopTagDetectionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasCameraPermission = remember { mutableStateOf(false) }
    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }

    if (hasCameraPermission.value) {
        LaunchedEffect(Unit) {
            // Bind preview use case to camera
            onInitializePreview(preview, previewView)
        }
        DisposableEffect(Unit) {
            onDispose {
                onDisposePreview()
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Camera preview
            AndroidView({ previewView }, modifier = Modifier.fillMaxWidth())

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        onStartTagDetectionClicked()
                    }
                ) {
                    Text("Start Detection")
                }
                Button(
                    modifier = Modifier,
                    onClick = {
                        onStopTagDetectionClicked()
                    }
                ) {
                    Text("Stop Detection")
                }
            }
        }
    } else {
        // Request camera permissions
        CameraPermissionRequest(
            onPermissionGranted = {
                hasCameraPermission.value = true
            },
            onPermissionDenied = {
                hasCameraPermission.value = false
                Log.d("AprilTagScreen", "Camera permission denied.")
            }
        )
    }
}

@Composable
fun CameraPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

    val hasCameraPermission =
        checkSelfPermission(context, Manifest.permission.CAMERA) == PERMISSION_GRANTED

    if (hasCameraPermission) {
        onPermissionGranted()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Camera permission required",
                modifier = Modifier.align(Alignment.Center)
            )
            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Request Permission")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun AprilTagScreenPreview() {
    AprilTagScreen(
        onInsButtonClicked = { /*TODO*/ },
        onInitializePreview = { _, _ -> },
        onDisposePreview = { /*TODO*/ },
        onStartTagDetectionClicked = { /*TODO*/ },
        onStopTagDetectionClicked = { /*TODO*/ })
}
