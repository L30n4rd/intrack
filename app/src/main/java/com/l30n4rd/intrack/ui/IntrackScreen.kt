package com.l30n4rd.intrack.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.l30n4rd.intrack.R
import com.l30n4rd.intrack.ui.apriltag.AprilTagScreen
import com.l30n4rd.intrack.ui.ins.InsPositionScreen

enum class IntrackScreen(@StringRes val title: Int) {
    Ins(title = R.string.ins),
    AprilTag(title = R.string.april_tag)
}

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupcakeAppBar(
    currentScreen: IntrackScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

@Composable
fun IntrackApp(
    positionViewModel: PositionViewModel,
    navController: NavHostController = rememberNavController()
) {
    val startScreen = IntrackScreen.Ins.name
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = IntrackScreen.valueOf(
        backStackEntry?.destination?.route ?: startScreen
    )

    Scaffold(
        topBar = {
            CupcakeAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = startScreen
        ) {
            composable(route = IntrackScreen.Ins.name) {
                InsPositionScreen(
                    viewModel = positionViewModel,
                    onAprilTagButtonClicked = {
                        navController.navigate(IntrackScreen.AprilTag.name)
                    }
                )
            }
            composable(route = IntrackScreen.AprilTag.name) {
                AprilTagScreen(
                    onInsButtonClicked = {
                        navController.navigate(IntrackScreen.Ins.name)
                    },
                    onStartTagDetectionClicked = {
                        positionViewModel.startTagDetection()
                    },
                    onStopTagDetectionClicked = {
                        positionViewModel.stopTagDetection()
                    },
                    onInitializePreview = { preview, previewView ->
                        positionViewModel.bindPreviewUseCase(
                            preview = preview,
                            previewView = previewView
                        )
                    },
                    onDisposePreview = {
                        positionViewModel.unbindPreviewUseCase()
                    }
                )
            }
        }
    }
}
