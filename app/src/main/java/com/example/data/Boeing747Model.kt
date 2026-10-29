package com.example.data

import androidx.compose.ui.graphics.Color

/**
 * 3D line segment of the airplane wireframe / structural mesh.
 */
data class ModelSegment(
    val start: Vector3D,
    val end: Vector3D,
    val color: Color,
    val strokeWidth: Float = 1.8f
)

/**
 * 3D facet (triangle) for solid/shaded airplane rendering.
 */
data class ModelFacet(
    val v1: Vector3D,
    val v2: Vector3D,
    val v3: Vector3D,
    val color: Color,
    val isRedTail: Boolean = false
)

/**
 * Procedural 3D model definition for Boeing 747-SR46.
 * Model coordinates:
 * +Z is Forward (Nose)
 * +X is Starboard (Right Wing)
 * +Y is Up (Cabin Ceiling / Fin)
 */
object Boeing747Model {

    const val MODEL_SCALE = 1.35

    // Fuselage parameters
    private const val NOSE_Z = 6.2
    private const val COCKPIT_Z = 4.8
    private const val HUMP_START_Z = 4.5
    private const val HUMP_PEAK_Z = 2.0
    private const val HUMP_END_Z = -0.5
    private const val TAIL_START_Z = -3.5
    private const val TAIL_TIP_Z = -7.0

    private const val FUSELAGE_RADIUS = 0.95
    private const val HUMP_HEIGHT = 1.55

    // Wings
    private const val WING_ROOT_FRONT_Z = 1.8
    private const val WING_ROOT_BACK_Z = -2.5
    private const val WING_TIP_Z = -5.8
    private const val WING_SPAN = 8.5
    private const val WING_DIHEDRAL = 0.75

    // Tailplanes
    private const val STAB_ROOT_Z = -4.5
    private const val STAB_TIP_Z = -7.2
    private const val STAB_SPAN = 3.6
    private const val FIN_BASE_FRONT_Z = -3.2
    private const val FIN_BASE_BACK_Z = -6.8
    private const val FIN_TIP_Z = -6.2
    private const val FIN_FULL_HEIGHT = 4.2
    private const val FIN_DAMAGED_HEIGHT = 1.3

    /**
     * Generates the 3D line segments for the Boeing 747 transformed into World Space.
     */
    fun getTransformedSegments(
        state: InterpolatedFlightState,
        scale: Double = MODEL_SCALE
    ): List<Pair<Vector3D, Vector3D>> {
        val baseSegments = mutableListOf<Pair<Vector3D, Vector3D>>()

        val white = Color(0xFFF1F5F9)
        val metal = Color(0xFF64748B)

        // 1. Fuselage longitudinal spines (Keel, Left/Right equator, Cabin roof)
        val nose = Vector3D(0.0, 0.0, NOSE_Z)
        val tailTip = Vector3D(0.0, 0.2, TAIL_TIP_Z)

        // Cabin roof profile (incorporating upper deck hump)
        val roofNose = Vector3D(0.0, 0.5, COCKPIT_Z)
        val roofHump = Vector3D(0.0, HUMP_HEIGHT, HUMP_PEAK_Z)
        val roofMid = Vector3D(0.0, FUSELAGE_RADIUS, HUMP_END_Z)
        val roofTail = Vector3D(0.0, FUSELAGE_RADIUS * 0.9, TAIL_START_Z)

        baseSegments.add(nose to roofNose)
        baseSegments.add(roofNose to roofHump)
        baseSegments.add(roofHump to roofMid)
        baseSegments.add(roofMid to roofTail)
        baseSegments.add(roofTail to tailTip)

        // Keel (bottom spine)
        val keelFront = Vector3D(0.0, -FUSELAGE_RADIUS * 0.6, COCKPIT_Z)
        val keelMid = Vector3D(0.0, -FUSELAGE_RADIUS, 0.0)
        val keelTail = Vector3D(0.0, -FUSELAGE_RADIUS * 0.4, TAIL_START_Z)
        baseSegments.add(nose to keelFront)
        baseSegments.add(keelFront to keelMid)
        baseSegments.add(keelMid to keelTail)
        baseSegments.add(keelTail to tailTip)

        // Left and right equator spines
        val leftMid = Vector3D(-FUSELAGE_RADIUS, 0.0, 0.0)
        val rightMid = Vector3D(FUSELAGE_RADIUS, 0.0, 0.0)
        baseSegments.add(nose to leftMid)
        baseSegments.add(nose to rightMid)
        baseSegments.add(leftMid to tailTip)
        baseSegments.add(rightMid to tailTip)

        // Fuselage cross ribs
        listOf(COCKPIT_Z, HUMP_PEAK_Z, 0.0, TAIL_START_Z).forEach { z ->
            val rad = if (z == TAIL_START_Z) FUSELAGE_RADIUS * 0.75 else FUSELAGE_RADIUS
            val topY = if (z == HUMP_PEAK_Z) HUMP_HEIGHT else rad
            val pTop = Vector3D(0.0, topY, z)
            val pBottom = Vector3D(0.0, -rad, z)
            val pLeft = Vector3D(-rad, 0.0, z)
            val pRight = Vector3D(rad, 0.0, z)

            baseSegments.add(pTop to pRight)
            baseSegments.add(pRight to pBottom)
            baseSegments.add(pBottom to pLeft)
            baseSegments.add(pLeft to pTop)
        }

        // 2. Main Wings (37.5° sweep)
        // Right wing
        val wingRootFrontR = Vector3D(FUSELAGE_RADIUS, -0.1, WING_ROOT_FRONT_Z)
        val wingRootBackR = Vector3D(FUSELAGE_RADIUS, -0.1, WING_ROOT_BACK_Z)
        val wingTipFrontR = Vector3D(WING_SPAN, WING_DIHEDRAL, WING_TIP_Z)
        val wingTipBackR = Vector3D(WING_SPAN - 0.4, WING_DIHEDRAL, WING_TIP_Z - 1.0)

        baseSegments.add(wingRootFrontR to wingTipFrontR)
        baseSegments.add(wingTipFrontR to wingTipBackR)
        baseSegments.add(wingTipBackR to wingRootBackR)

        // Left wing
        val wingRootFrontL = Vector3D(-FUSELAGE_RADIUS, -0.1, WING_ROOT_FRONT_Z)
        val wingRootBackL = Vector3D(-FUSELAGE_RADIUS, -0.1, WING_ROOT_BACK_Z)
        val wingTipFrontL = Vector3D(-WING_SPAN, WING_DIHEDRAL, WING_TIP_Z)
        val wingTipBackL = Vector3D(-WING_SPAN + 0.4, WING_DIHEDRAL, WING_TIP_Z - 1.0)

        baseSegments.add(wingRootFrontL to wingTipFrontL)
        baseSegments.add(wingTipFrontL to wingTipBackL)
        baseSegments.add(wingTipBackL to wingRootBackL)

        // 3. Four Pratt & Whitney JT9D turbofan engines
        val engineXPositions = listOf(2.8, 5.6, -2.8, -5.6)
        val engineZPositions = listOf(-0.8, -2.6, -0.8, -2.6)
        val engineYPositions = listOf(-0.6, -0.2, -0.6, -0.2)

        for (i in 0..3) {
            val ex = engineXPositions[i]
            val ez = engineZPositions[i]
            val ey = engineYPositions[i]
            val frontCenter = Vector3D(ex, ey, ez + 0.9)
            val backCenter = Vector3D(ex, ey, ez - 0.9)
            baseSegments.add(frontCenter to backCenter)

            // Pylon to wing
            val wingAttachment = Vector3D(ex, ey + 0.5, ez)
            baseSegments.add(Vector3D(ex, ey, ez) to wingAttachment)
        }

        // 4. Horizontal Stabilizers
        val stabRootR = Vector3D(0.4, 0.4, STAB_ROOT_Z)
        val stabTipR = Vector3D(STAB_SPAN, 0.8, STAB_TIP_Z)
        val stabBackR = Vector3D(STAB_SPAN - 0.3, 0.8, STAB_TIP_Z - 0.8)
        baseSegments.add(stabRootR to stabTipR)
        baseSegments.add(stabTipR to stabBackR)
        baseSegments.add(stabBackR to Vector3D(0.2, 0.4, TAIL_TIP_Z))

        val stabRootL = Vector3D(-0.4, 0.4, STAB_ROOT_Z)
        val stabTipL = Vector3D(-STAB_SPAN, 0.8, STAB_TIP_Z)
        val stabBackL = Vector3D(-STAB_SPAN + 0.3, 0.8, STAB_TIP_Z - 0.8)
        baseSegments.add(stabRootL to stabTipL)
        baseSegments.add(stabTipL to stabBackL)
        baseSegments.add(stabBackL to Vector3D(-0.2, 0.4, TAIL_TIP_Z))

        // 5. Vertical Stabilizer (JAL Red Tail)
        val finBaseFront = Vector3D(0.0, 0.8, FIN_BASE_FRONT_Z)
        val finBaseBack = Vector3D(0.0, 0.8, FIN_BASE_BACK_Z)

        // After decompression at 18:24:35, 5m of upper vertical stabilizer was severed
        val currentFinHeight = if (state.isDecompressed) FIN_DAMAGED_HEIGHT else FIN_FULL_HEIGHT
        val finTip = Vector3D(0.0, currentFinHeight, FIN_TIP_Z)
        val finTipBack = Vector3D(0.0, currentFinHeight - 0.2, FIN_TIP_Z - 1.0)

        baseSegments.add(finBaseFront to finTip)
        baseSegments.add(finTip to finTipBack)
        baseSegments.add(finTipBack to finBaseBack)
        baseSegments.add(finBaseBack to finBaseFront)

        // Transform all local segments into World 3D coordinates using aircraft frame
        val pos = state.position3D
        val fwd = state.forwardVector
        val right = state.rightVector
        val up = state.upVector

        return baseSegments.map { (p1, p2) ->
            val w1 = transformLocalToWorld(p1, pos, fwd, right, up, scale)
            val w2 = transformLocalToWorld(p2, pos, fwd, right, up, scale)
            w1 to w2
        }
    }

    /**
     * Transform a single local model coordinate into world space.
     */
    fun transformLocalToWorld(
        local: Vector3D,
        origin: Vector3D,
        forward: Vector3D,
        right: Vector3D,
        up: Vector3D,
        scale: Double
    ): Vector3D {
        val scaled = local * scale
        return origin + (right * scaled.x) + (up * scaled.y) + (forward * scaled.z)
    }

    /**
     * Strobe & Navigation light world positions.
     */
    fun getStrobeLightPositions(
        state: InterpolatedFlightState,
        scale: Double = MODEL_SCALE
    ): Triple<Vector3D, Vector3D, Vector3D> {
        val pos = state.position3D
        val fwd = state.forwardVector
        val right = state.rightVector
        val up = state.upVector

        val beaconLocal = Vector3D(0.0, HUMP_HEIGHT + 0.15, HUMP_PEAK_Z)
        val navGreenLocal = Vector3D(WING_SPAN, WING_DIHEDRAL, WING_TIP_Z)
        val navRedLocal = Vector3D(-WING_SPAN, WING_DIHEDRAL, WING_TIP_Z)

        return Triple(
            transformLocalToWorld(beaconLocal, pos, fwd, right, up, scale),
            transformLocalToWorld(navGreenLocal, pos, fwd, right, up, scale),
            transformLocalToWorld(navRedLocal, pos, fwd, right, up, scale)
        )
    }
}
