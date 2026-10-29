package com.example.data

import kotlin.math.*

/**
 * 3D Vector representation for spatial flight geometry.
 */
data class Vector3D(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double) = Vector3D(x / scalar, y / scalar, z / scalar)

    fun length(): Double = sqrt(x * x + y * y + z * z)

    fun normalize(): Vector3D {
        val len = length()
        return if (len > 1e-6) this / len else Vector3D(0.0, 0.0, 1.0)
    }

    fun dot(other: Vector3D): Double = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
}

/**
 * Real-time interpolated flight telemetry state.
 */
data class InterpolatedFlightState(
    val secondsFromStart: Double,
    val timeClockStr: String,
    val lon: Double,
    val lat: Double,
    val altFt: Double,
    val speedKt: Int,
    val vsFpm: Int,
    val event: String,
    val progress: Float,
    val position3D: Vector3D,
    val forwardVector: Vector3D,
    val rightVector: Vector3D,
    val upVector: Vector3D,
    val bankAngleDeg: Double,
    val pitchAngleDeg: Double,
    val isDecompressed: Boolean,
    val isGpwsAlarm: Boolean,
    val waypointIndex: Int
)

enum class CameraPerspective {
    FREE_ORBIT,
    CHASE_B747,
    TACTICAL_MAP,
    PROFILE_SIDE
}

enum class TerrainTextureMode {
    SATELLITE_COLOR,
    GRAYSCALE_HEIGHTMAP,
    TACTICAL_GRID
}

object TrajectoryMath {

    const val CENTER_LON = 139.10
    const val CENTER_LAT = 35.40
    const val HORIZONTAL_SCALE = 220.0 // 3D units per degree
    const val ALTITUDE_SCALE = 0.0022  // 3D units per foot (approx 2.4x vertical exaggeration)

    val WAYPOINT_SECONDS: List<Int> = FlightData.WAYPOINTS.map {
        FlightData.parseTimeSeconds(it.timeStr) - FlightData.T0_SECONDS
    }

    val WAYPOINT_POINTS_3D: List<Vector3D> = FlightData.WAYPOINTS.map { wp ->
        geoTo3D(wp.lon, wp.lat, wp.altFt)
    }

    fun geoTo3D(lon: Double, lat: Double, altFt: Double): Vector3D {
        val x = (lon - CENTER_LON) * HORIZONTAL_SCALE
        val z = -(lat - CENTER_LAT) * HORIZONTAL_SCALE
        val y = altFt * ALTITUDE_SCALE
        return Vector3D(x, y, z)
    }

    const val TERRAIN_BOUNDS = 165.0 // -165 to +165 in 3D world space (330x330 region)

    /**
     * Compute geographical elevation in 3D world coordinates (Y units)
     * at 3D spatial location (x, z).
     */
    fun getTerrainElevation(x: Double, z: Double): Double {
        val lon = CENTER_LON + (x / HORIZONTAL_SCALE)
        val lat = CENTER_LAT - (z / HORIZONTAL_SCALE)

        // Water bodies check (Sagami Bay, Suruga Bay, Tokyo Bay, Pacific Ocean)
        if (lat < 35.15 && lon in 138.45..138.85) {
            val bayLimit = 35.15 - ((lon - 138.65) / 0.22) * ((lon - 138.65) / 0.22) * 0.45
            if (lat < bayLimit) return 0.0 // Suruga Bay
        }
        if (lat < 35.32 && lon in 139.12..139.60) {
            val bayLimit = 35.32 - ((lon - 139.36) / 0.26) * ((lon - 139.36) / 0.26) * 0.55
            if (lat < bayLimit) return 0.0 // Sagami Bay
        }
        if (lat in 35.25..35.68 && lon >= 139.70) {
            return 0.0 // Tokyo Bay
        }
        if (lat < 34.68) return 0.0 // Pacific Ocean

        // Mount Fuji (3,776m = ~8.3 world units)
        val dFuji = hypot(lon - 138.727, (lat - 35.360) * 1.1)
        if (dFuji < 0.22) {
            val fujiM = 3776.0 * exp(-(dFuji / 0.075).pow(1.6))
            return (fujiM * 3.28084 * ALTITUDE_SCALE).coerceAtLeast(0.1)
        }

        // Mount Takamagahara / Osutaka Ridge (1,979m = ~4.35 world units)
        val dTaka = hypot(lon - 138.693, lat - 35.998)
        if (dTaka < 0.20) {
            val takaM = 1979.0 * exp(-(dTaka / 0.08).pow(1.4))
            return (takaM * 3.28084 * ALTITUDE_SCALE).coerceAtLeast(0.1)
        }

        // Mountain ridges (Chichibu / Gunma / Okutama)
        if (lat > 35.55 && lon < 139.25) {
            val dNorth = (lat - 35.55) / 0.6
            val ridgePattern = sin(x * 0.12) * cos(z * 0.12)
            val elevM = 400.0 + dNorth * 900.0 + ridgePattern * 250.0
            return (elevM * 3.28084 * ALTITUDE_SCALE).coerceIn(0.1, 4.2)
        }

        // Tanzawa mountains north of Sagami Bay
        val dTanzawa = hypot(lon - 139.15, lat - 35.46)
        if (dTanzawa < 0.16) {
            val tanzawaM = 1673.0 * exp(-(dTanzawa / 0.07).pow(1.5))
            return (tanzawaM * 3.28084 * ALTITUDE_SCALE).coerceAtLeast(0.1)
        }

        // Southern Alps (west)
        if (lon < 138.55) {
            val dWest = (138.55 - lon) / 0.20
            val elevM = 600.0 + dWest * 1800.0
            return (elevM * 3.28084 * ALTITUDE_SCALE).coerceIn(0.1, 5.5)
        }

        // Lowland plains (Kanto plain)
        return 0.1
    }

    /**
     * Catmull-Rom spline interpolation between 4 control points.
     */
    fun catmullRom(p0: Vector3D, p1: Vector3D, p2: Vector3D, p3: Vector3D, t: Double): Vector3D {
        val t2 = t * t
        val t3 = t2 * t

        val f0 = -0.5 * t3 + t2 - 0.5 * t
        val f1 = 1.5 * t3 - 2.5 * t2 + 1.0
        val f2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t
        val f3 = 0.5 * t3 - 0.5 * t2

        return Vector3D(
            p0.x * f0 + p1.x * f1 + p2.x * f2 + p3.x * f3,
            p0.y * f0 + p1.y * f1 + p2.y * f2 + p3.y * f3,
            p0.z * f0 + p1.z * f1 + p2.z * f2 + p3.z * f3
        )
    }

    /**
     * Evaluate position on the flight path at normalized parameter u in [0, 1].
     */
    fun evaluateSpline(u: Double): Vector3D {
        val clampedU = u.coerceIn(0.0, 1.0)
        val n = WAYPOINT_POINTS_3D.size
        if (n < 2) return WAYPOINT_POINTS_3D.firstOrNull() ?: Vector3D(0.0, 0.0, 0.0)

        val totalSegments = n - 1
        val scaled = clampedU * totalSegments
        val segment = min(scaled.toInt(), totalSegments - 1)
        val t = scaled - segment

        val p0 = WAYPOINT_POINTS_3D[(segment - 1).coerceAtLeast(0)]
        val p1 = WAYPOINT_POINTS_3D[segment]
        val p2 = WAYPOINT_POINTS_3D[(segment + 1).coerceAtMost(n - 1)]
        val p3 = WAYPOINT_POINTS_3D[(segment + 2).coerceAtMost(n - 1)]

        return catmullRom(p0, p1, p2, p3, t)
    }

    /**
     * Compute tangent vector at parameter u.
     */
    fun evaluateTangent(u: Double): Vector3D {
        val delta = 0.001
        val u1 = (u - delta).coerceAtLeast(0.0)
        val u2 = (u + delta).coerceAtMost(1.0)
        val p1 = evaluateSpline(u1)
        val p2 = evaluateSpline(u2)
        return (p2 - p1).normalize()
    }

    /**
     * Calculate comprehensive flight state for a specific elapsed second.
     */
    fun getFlightState(sec: Double): InterpolatedFlightState {
        val totalSec = FlightData.TOTAL_FLIGHT_SECONDS.toDouble()
        val clampedSec = sec.coerceIn(0.0, totalSec)
        val progress = (clampedSec / totalSec).toFloat()

        // Find waypoint bracket
        var idx = 0
        while (idx < WAYPOINT_SECONDS.size - 1 && WAYPOINT_SECONDS[idx + 1] < clampedSec) {
            idx++
        }
        val w0 = FlightData.WAYPOINTS[idx]
        val w1 = FlightData.WAYPOINTS[(idx + 1).coerceAtMost(FlightData.WAYPOINTS.size - 1)]
        val t0Sec = WAYPOINT_SECONDS[idx]
        val t1Sec = WAYPOINT_SECONDS[(idx + 1).coerceAtMost(WAYPOINT_SECONDS.size - 1)]

        val span = (t1Sec - t0Sec).coerceAtLeast(1)
        val factor = ((clampedSec - t0Sec) / span).coerceIn(0.0, 1.0)

        val lon = w0.lon + (w1.lon - w0.lon) * factor
        val lat = w0.lat + (w1.lat - w0.lat) * factor
        val altFt = w0.altFt + (w1.altFt - w0.altFt) * factor
        val speedKt = (w0.speedKt + (w1.speedKt - w0.speedKt) * factor).roundToInt()
        val vsFpm = (((w1.altFt - w0.altFt) / span) * 60.0).roundToInt()
        val event = if (factor > 0.5) w1.event else w0.event

        // 3D position & forward tangent
        val u = (clampedSec / totalSec).coerceIn(0.0001, 0.9999)
        val pos = evaluateSpline(u)
        val forward = evaluateTangent(u)

        // Turning curvature & banking calculation
        val uAhead = (u + 0.005).coerceAtMost(0.9999)
        val forwardAhead = evaluateTangent(uAhead)
        val headingNow = atan2(forward.x, -forward.z)
        val headingAhead = atan2(forwardAhead.x, -forwardAhead.z)
        var headingDiff = headingAhead - headingNow
        if (headingDiff > PI) headingDiff -= 2 * PI
        if (headingDiff < -PI) headingDiff += 2 * PI

        var bankAngleRad = -headingDiff * 40.0
        bankAngleRad = bankAngleRad.coerceIn(-0.65, 0.65) // Clamped to ~37 degrees

        // Post-decompression oscillations (640s = 18:24:35)
        var dutchRollRad = 0.0
        var phugoidPitchRad = 0.0
        val isDecompressed = clampedSec >= 640.0
        val isGpwsAlarm = clampedSec >= 2520.0

        if (isDecompressed && clampedSec < totalSec - 5.0) {
            val oscTime = clampedSec * 2.8
            dutchRollRad = sin(oscTime * 0.9) * 0.22      // lateral Dutch roll wobble
            phugoidPitchRad = cos(oscTime * 0.45) * 0.08  // vertical Phugoid hunting
        }

        // Natural pitch from vertical trajectory slope
        val horizontalSpeed = sqrt(forward.x * forward.x + forward.z * forward.z)
        val pathPitchRad = atan2(forward.y, horizontalSpeed)
        val totalPitchRad = pathPitchRad + phugoidPitchRad
        val totalBankRad = bankAngleRad + dutchRollRad

        // Build orthonormal frame
        val worldUp = Vector3D(0.0, 1.0, 0.0)
        var localRight = worldUp.cross(forward).normalize()
        if (localRight.length() < 0.001) localRight = Vector3D(1.0, 0.0, 0.0)
        val localUp = forward.cross(localRight).normalize()

        // Apply roll rotation around forward axis
        val cosB = cos(totalBankRad)
        val sinB = sin(totalBankRad)
        val rolledRight = (localRight * cosB + localUp * sinB).normalize()
        val rolledUp = forward.cross(rolledRight).normalize()

        // Time clock format
        val currentAbsSec = FlightData.T0_SECONDS + clampedSec.toInt()
        val h = currentAbsSec / 3600
        val m = (currentAbsSec % 3600) / 60
        val s = currentAbsSec % 60
        val clockStr = "%02d:%02d:%02d".format(h, m, s)

        return InterpolatedFlightState(
            secondsFromStart = clampedSec,
            timeClockStr = clockStr,
            lon = lon,
            lat = lat,
            altFt = altFt,
            speedKt = speedKt,
            vsFpm = vsFpm,
            event = event,
            progress = progress,
            position3D = pos,
            forwardVector = forward,
            rightVector = rolledRight,
            upVector = rolledUp,
            bankAngleDeg = Math.toDegrees(totalBankRad),
            pitchAngleDeg = Math.toDegrees(totalPitchRad),
            isDecompressed = isDecompressed,
            isGpwsAlarm = isGpwsAlarm,
            waypointIndex = idx
        )
    }

    /**
     * Precomputed trajectory ribbon segments for high-speed canvas drawing.
     */
    const val NUM_SPLINE_SAMPLES = 260
    val PRECOMPUTED_POINTS: List<Vector3D> = (0..NUM_SPLINE_SAMPLES).map { i ->
        evaluateSpline(i.toDouble() / NUM_SPLINE_SAMPLES)
    }
}
