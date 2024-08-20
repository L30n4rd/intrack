package com.l30n4rd.intrack.ui.ins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.l30n4rd.intrack.ui.PositionViewModel


@Composable
fun InsPositionScreen(
    viewModel: PositionViewModel,
    onAprilTagButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                modifier = Modifier,
                onClick = {
                    onAprilTagButtonClicked()
                }
            ) {
                Text("AprilTag Screen")
            }
        }

        // Raw values representation
        RawPositionValues(
            modifier = Modifier,
            position = viewModel.uiState.collectAsState().value.transformationMatrix
        )

        // Draw the current position on a coordinate system
        CoordinateSystem(
            modifier = Modifier
                .fillMaxWidth(0.3f),
            position = viewModel.uiState.collectAsState().value.transformationMatrix
        )

        // Position recording
        PositionRecording(
            onStart = { viewModel.startPositionRecording() },
            onStop = { viewModel.stopPositionRecordingAndShare(context) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp),
        )

        // AprilTag recording
        AprilTagRecording(
            onStart = { viewModel.startAprilTagRecording() },
            onStop = { viewModel.stopAprilTagRecording(context) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp),
        )

        // Ground truth recording
        GroundTruthRecording(
            onStart = { viewModel.startGroundTruthRecording() },
            onAddCheckPoint = { viewModel.addNextGroundTruthPoint() },
            onStop = { viewModel.stopPositionRecordingAndShare(context) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp),
        )

        // Step width calculation
        StepWidthCalculation(
            onStart = { viewModel.startStepWidthCalculation() },
            onStop =  { viewModel.stopStepWidthCalculation() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp),
        )
    }
}

@Composable
fun GroundTruthRecording(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onAddCheckPoint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Ground Truth Recording", style = TextStyle(fontWeight = FontWeight.Bold))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart) {
                Text("Start")
            }
            Button(onClick = onAddCheckPoint) {
                Text("Add Checkpoint")
            }
            Button(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun StepWidthCalculation(
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Step Width Calculation", style = TextStyle(fontWeight = FontWeight.Bold))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart) {
                Text("Start")
            }
            Button(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun PositionRecording(
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Position Recording", style = TextStyle(fontWeight = FontWeight.Bold))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart) {
                Text("Start")
            }
            Button(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Composable
fun AprilTagRecording(
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("AprilTag Recording", style = TextStyle(fontWeight = FontWeight.Bold))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart) {
                Text("Start")
            }
            Button(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Preview
@Composable
fun GroundTruthButtonsPreview() {
    GroundTruthRecording({}, {}, {})
}
