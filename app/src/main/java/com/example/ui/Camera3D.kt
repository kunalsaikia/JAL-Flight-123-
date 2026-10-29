package com.example.ui

import androidx.compose.ui.geometry.Offset
import com.example.data.CameraPerspective
import com.example.data.InterpolatedFlightState
import com.example.data.Vector3D
import kotlin.math.*

/**
 * Camera state for 3D spatial transformation and perspective projection.
 */
data class Camera3DState(
    val azimuthDeg: Float = 35f,
    val elevationDeg: Float = 32f,
    val distance: Float = 145f,
    val target: Vector3D = Vector3D(0.0, 15.0, 0.0),
    val perspective: CameraPerspective = CameraPerspective.FREE_ORBIT
) {
    /**
     * Compute current camera eye position in world space.
     */
    fun computeEyePosition(state: InterpolatedFlightState): Vector3D {
        return when (perspective) {
            CameraPerspective.CHASE_B747 -> {
                val planePos = state.position3D
                val fwd = state.forwardVector
                val up = state.upVector
                // Sit behind and slightly above the Boeing 747
                planePos - (fwd * 28.0) + (up * 8.5)
            }
            CameraPerspective.TACTICAL_MAP -> {
                val t = target
                Vector3D(t.x, t.y + distance * 1.5, t.z + 0.1)
            }
            CameraPerspective.PROFILE_SIDE -> {
                val t = target
                Vector3D(t.x, t.y + 12.0, t.z + distance)
            }
            CameraPerspective.FREE_ORBIT -> {
                val radAz = Math.toRadians(azimuthDeg.toDouble())
                val radEl = Math.toRadians(elevationDeg.toDouble().coerceIn(-89.0, 89.0))
                val cosEl = cos(radEl)
                val sinEl = sin(radEl)
                val cx = target.x + distance * cosEl * sin(radAz)
                val cy = target.y + distance * sinEl
                val cz = target.z + distance * cosEl * cos(radAz)
                Vector3D(cx, cy, cz)
            }
        }
    }

    /**
     * Compute camera look-at target.
     */
    fun computeLookAtTarget(state: InterpolatedFlightState): Vector3D {
        return when (perspective) {
            CameraPerspective.CHASE_B747 -> {
                val planePos = state.position3D
                val fwd = state.forwardVector
                planePos + (fwd * 15.0)
            }
            CameraPerspective.TACTICAL_MAP -> state.position3D
            CameraPerspective.PROFILE_SIDE -> state.position3D
            CameraPerspective.FREE_ORBIT -> target
        }
    }
}

/**
 * 3D to 2D Perspective Projection engine.
 */
class PerspectiveProjector(
    private val eye: Vector3D,
    private val lookAt: Vector3D,
    private val screenWidth: Float,
    private val screenHeight: Float,
    fovDegrees: Double = 52.0
) {
    private val camForward: Vector3D = (lookAt - eye).normalize()
    private val camRight: Vector3D
    private val camUp: Vector3D
    private val focalLength: Double

    init {
        val worldUp = Vector3D(0.0, 1.0, 0.0)
        var right = camForward.cross(worldUp)
        if (right.length() < 1e-4) {
            // Singularity when looking vertically down
            right = Vector3D(1.0, 0.0, 0.0)
        }
        camRight = right.normalize()
        camUp = camRight.cross(camForward).normalize()

        val fovRad = Math.toRadians(fovDegrees)
        focalLength = (screenWidth.coerceAtLeast(300f) * 0.5) / tan(fovRad * 0.5)
    }

    data class ProjectedPoint(
        val offset: Offset,
        val depth: Double,
        val isVisible: Boolean
    )

    fun project(point: Vector3D): ProjectedPoint {
        val v = point - eye
        val zCam = v.dot(camForward)
        if (zCam <= 1.0) {
            return ProjectedPoint(Offset.Zero, zCam, isVisible = false)
        }

        val xCam = v.dot(camRight)
        val yCam = v.dot(camUp)

        val factor = focalLength / zCam
        val sx = (screenWidth * 0.5f) + (xCam * factor).toFloat()
        val sy = (screenHeight * 0.5f) - (yCam * factor).toFloat()

        val isVisible = sx in -200f..(screenWidth + 200f) && sy in -200f..(screenHeight + 200f)
        return ProjectedPoint(Offset(sx, sy), zCam, isVisible)
    }
}
