package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FlightSimulationUiState(
    val currentSeconds: Double = 0.0,
    val isPlaying: Boolean = true,
    val playbackSpeed: Float = 3f,
    val cameraState: Camera3DState = Camera3DState(),
    val flightState: InterpolatedFlightState = TrajectoryMath.getFlightState(0.0),
    val isDetailsSheetVisible: Boolean = false,
    val isPfdInstrumentsVisible: Boolean = true,
    val followAircraft: Boolean = false,
    val terrainMode: TerrainTextureMode = TerrainTextureMode.SATELLITE_COLOR
)

class FlightSimulationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FlightSimulationUiState())
    val uiState: StateFlow<FlightSimulationUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var lastFrameTime = System.nanoTime()

    init {
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        lastFrameTime = System.nanoTime()

        tickerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.nanoTime()
                val deltaSec = (now - lastFrameTime) / 1_000_000_000.0
                lastFrameTime = now

                if (_uiState.value.isPlaying) {
                    val speed = _uiState.value.playbackSpeed
                    val newSec = _uiState.value.currentSeconds + (deltaSec * speed)
                    val maxSec = FlightData.TOTAL_FLIGHT_SECONDS.toDouble()

                    if (newSec >= maxSec) {
                        val finalState = TrajectoryMath.getFlightState(maxSec)
                        _uiState.update {
                            it.copy(
                                currentSeconds = maxSec,
                                isPlaying = false,
                                flightState = finalState
                            )
                        }
                    } else {
                        val state = TrajectoryMath.getFlightState(newSec)
                        _uiState.update {
                            val updatedCamera = if (it.followAircraft && it.cameraState.perspective == CameraPerspective.FREE_ORBIT) {
                                it.cameraState.copy(target = state.position3D)
                            } else {
                                it.cameraState
                            }
                            it.copy(
                                currentSeconds = newSec,
                                flightState = state,
                                cameraState = updatedCamera
                            )
                        }
                    }
                }
                delay(16) // ~60fps
            }
        }
    }

    fun togglePlayPause() {
        _uiState.update {
            val nextPlaying = !it.isPlaying
            if (nextPlaying && it.currentSeconds >= FlightData.TOTAL_FLIGHT_SECONDS) {
                lastFrameTime = System.nanoTime()
                val s0 = TrajectoryMath.getFlightState(0.0)
                it.copy(isPlaying = true, currentSeconds = 0.0, flightState = s0)
            } else {
                lastFrameTime = System.nanoTime()
                it.copy(isPlaying = nextPlaying)
            }
        }
    }

    fun reset() {
        lastFrameTime = System.nanoTime()
        val s0 = TrajectoryMath.getFlightState(0.0)
        _uiState.update {
            it.copy(
                currentSeconds = 0.0,
                flightState = s0,
                isPlaying = false
            )
        }
    }

    fun seekToSeconds(seconds: Double) {
        val clamped = seconds.coerceIn(0.0, FlightData.TOTAL_FLIGHT_SECONDS.toDouble())
        val state = TrajectoryMath.getFlightState(clamped)
        lastFrameTime = System.nanoTime()
        _uiState.update {
            it.copy(
                currentSeconds = clamped,
                flightState = state
            )
        }
    }

    fun seekToProgress(progress: Float) {
        val totalSec = FlightData.TOTAL_FLIGHT_SECONDS.toDouble()
        seekToSeconds(progress * totalSec)
    }

    fun setSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setCameraPerspective(perspective: CameraPerspective) {
        _uiState.update { current ->
            val updatedCamera = when (perspective) {
                CameraPerspective.TACTICAL_MAP -> current.cameraState.copy(
                    perspective = perspective,
                    azimuthDeg = 0f,
                    elevationDeg = 89f,
                    distance = 220f,
                    target = current.flightState.position3D
                )
                CameraPerspective.PROFILE_SIDE -> current.cameraState.copy(
                    perspective = perspective,
                    azimuthDeg = 90f,
                    elevationDeg = 6f,
                    distance = 180f,
                    target = Vector3D(0.0, 20.0, 0.0)
                )
                CameraPerspective.CHASE_B747 -> current.cameraState.copy(
                    perspective = perspective
                )
                CameraPerspective.FREE_ORBIT -> current.cameraState.copy(
                    perspective = perspective,
                    azimuthDeg = 35f,
                    elevationDeg = 30f,
                    distance = 150f,
                    target = current.flightState.position3D
                )
            }
            current.copy(cameraState = updatedCamera)
        }
    }

    fun jumpToWaypoint(index: Int) {
        if (index in FlightData.WAYPOINTS.indices) {
            val sec = TrajectoryMath.WAYPOINT_SECONDS[index].toDouble()
            seekToSeconds(sec)
        }
    }

    fun onCameraOrbit(deltaAzimuth: Float, deltaElevation: Float) {
        if (_uiState.value.cameraState.perspective != CameraPerspective.FREE_ORBIT) {
            // Switch to free orbit if user touches to rotate
            setCameraPerspective(CameraPerspective.FREE_ORBIT)
        }
        _uiState.update { current ->
            val cam = current.cameraState
            val newAz = (cam.azimuthDeg + deltaAzimuth) % 360f
            val newEl = (cam.elevationDeg + deltaElevation).coerceIn(4f, 88f)
            current.copy(cameraState = cam.copy(azimuthDeg = newAz, elevationDeg = newEl))
        }
    }

    fun onCameraZoom(zoomFactor: Float) {
        _uiState.update { current ->
            val cam = current.cameraState
            val newDist = (cam.distance / zoomFactor).coerceIn(25f, 500f)
            current.copy(cameraState = cam.copy(distance = newDist))
        }
    }

    fun centerOnAircraft() {
        _uiState.update { current ->
            val planePos = current.flightState.position3D
            val cam = current.cameraState.copy(target = planePos)
            current.copy(cameraState = cam, followAircraft = true)
        }
    }

    fun toggleFollowAircraft() {
        _uiState.update { it.copy(followAircraft = !it.followAircraft) }
    }

    fun toggleDetailsSheet(show: Boolean) {
        _uiState.update { it.copy(isDetailsSheetVisible = show) }
    }

    fun togglePfdInstruments() {
        _uiState.update { it.copy(isPfdInstrumentsVisible = !it.isPfdInstrumentsVisible) }
    }

    fun setTerrainMode(mode: TerrainTextureMode) {
        _uiState.update { it.copy(terrainMode = mode) }
    }

    fun cycleTerrainMode() {
        _uiState.update {
            val nextMode = when (it.terrainMode) {
                TerrainTextureMode.SATELLITE_COLOR -> TerrainTextureMode.GRAYSCALE_HEIGHTMAP
                TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> TerrainTextureMode.TACTICAL_GRID
                TerrainTextureMode.TACTICAL_GRID -> TerrainTextureMode.SATELLITE_COLOR
            }
            it.copy(terrainMode = nextMode)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
