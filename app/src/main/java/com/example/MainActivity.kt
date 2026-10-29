package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FlightSimulationApp()
            }
        }
    }
}

@Composable
fun FlightSimulationApp(
    viewModel: FlightSimulationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle back button when details sheet is open
    BackHandler(enabled = uiState.isDetailsSheetVisible) {
        viewModel.toggleDetailsSheet(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("flight_simulation_root"),
        containerColor = CockpitBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CockpitBackground)
        ) {
            // 1. Fullscreen 3D Flight Canvas
            FlightCanvas3D(
                uiState = uiState,
                onOrbit = { dAz, dEl -> viewModel.onCameraOrbit(dAz, dEl) },
                onZoom = { zoom -> viewModel.onCameraZoom(zoom) },
                onDoubleTap = { viewModel.centerOnAircraft() },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Flight Header (Top Bar with Camera Views, Terrain Modes & Dossier Button)
            FlightHeader(
                currentPerspective = uiState.cameraState.perspective,
                terrainMode = uiState.terrainMode,
                followAircraft = uiState.followAircraft,
                onSelectPerspective = { viewModel.setCameraPerspective(it) },
                onToggleTerrainMode = { viewModel.cycleTerrainMode() },
                onCenterAircraft = { viewModel.centerOnAircraft() },
                onOpenInfo = { viewModel.toggleDetailsSheet(true) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )

            // 3. Telemetry HUD Card (Top-Left under header)
            TelemetryHudCard(
                flightState = uiState.flightState,
                isPfdVisible = uiState.isPfdInstrumentsVisible,
                onTogglePfd = { viewModel.togglePfdInstruments() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 58.dp, start = 12.dp)
            )

            // 4. Playback Controls Bar (Bottom)
            PlaybackControlsBar(
                isPlaying = uiState.isPlaying,
                playbackSpeed = uiState.playbackSpeed,
                flightState = uiState.flightState,
                onTogglePlay = { viewModel.togglePlayPause() },
                onReset = { viewModel.reset() },
                onSeekProgress = { viewModel.seekToProgress(it) },
                onSelectSpeed = { viewModel.setSpeed(it) },
                onJumpToWaypoint = { viewModel.jumpToWaypoint(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 5. Investigation Dossier & Radar Table Modal Sheet
            if (uiState.isDetailsSheetVisible) {
                FlightDetailsSheet(
                    onDismiss = { viewModel.toggleDetailsSheet(false) },
                    onSelectWaypoint = { index ->
                        viewModel.jumpToWaypoint(index)
                        viewModel.toggleDetailsSheet(false)
                    },
                    currentWaypointIndex = uiState.flightState.waypointIndex
                )
            }
        }
    }
}
