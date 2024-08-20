package com.l30n4rd.intrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.l30n4rd.intrack.ui.IntrackApp
import com.l30n4rd.intrack.ui.PositionViewModel
import com.l30n4rd.intrack.ui.theme.IntrackTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: PositionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setContext(this)
        viewModel.initializeCamera()

        setContent {
            IntrackTheme {
                IntrackApp(positionViewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // TODO: camera not initialized on first start
        viewModel.startTagDetection()
    }
}
