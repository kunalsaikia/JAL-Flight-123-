package com.example.data

/**
 * Data model for georeferenced radar and flight data recorder (FDR) telemetry points.
 */
data class FlightWaypoint(
    val timeStr: String,
    val lon: Double,
    val lat: Double,
    val altFt: Double,
    val speedKt: Int,
    val event: String,
    val isCritical: Boolean = false,
    val tag: String = ""
)

/**
 * Geographic landmark in central Honshu along the trajectory.
 */
data class FlightLandmark(
    val name: String,
    val lon: Double,
    val lat: Double,
    val elevationM: Double,
    val type: LandmarkType,
    val description: String
)

enum class LandmarkType {
    AIRPORT,
    MOUNTAIN,
    VOR_NAV,
    BAY,
    MILITARY_BASE,
    CRASH_SITE
}

/**
 * Historical milestones and flight dataset for Japan Air Lines Flight 123.
 * Aircraft: Boeing 747-SR46, Registration JA8119, Serial Number 20783.
 * Date: 12 August 1985 (18:13:55 - 18:56:30 JST)
 */
object FlightData {

    const val AIRCRAFT_REG = "JA8119"
    const val AIRCRAFT_TYPE = "Boeing 747-SR46"
    const val FLIGHT_CALLSIGN = "JAL 123"
    const val ROUTE = "Tokyo Haneda (HND) → Osaka Itami (ITM)"
    const val EVENT_DATE = "12 August 1985"
    const val PASSENGERS_CREW = "524 on board (509 passengers, 15 crew)"
    const val SURVIVORS = "4 survivors rescued"

    val WAYPOINTS: List<FlightWaypoint> = listOf(
        FlightWaypoint("18:13:55", 139.790, 35.535, 3200.0, 170, "Climb out from Tokyo Haneda (TIA) Runway 16L", isCritical = true, tag = "Haneda Departure"),
        FlightWaypoint("18:18:30", 139.730, 35.250, 12200.0, 290, "Climbing southbound over Miura Peninsula", isCritical = false, tag = "Miura Climb"),
        FlightWaypoint("18:21:38", 139.520, 35.080, 18900.0, 300, "Cruising west toward Sagami Bay waypoint XAC", isCritical = false, tag = "Sagami Cruise"),
        FlightWaypoint("18:24:12", 139.380, 34.930, 23400.0, 300, "Leveling off near Izu Oshima airspace", isCritical = false, tag = "Izu Oshima"),
        FlightWaypoint("18:24:35", 139.300, 34.880, 23900.0, 300, "CRITICAL: Bulkhead rupture, tail fin severed, hydraulic systems A/B/C/D lost", isCritical = true, tag = "Bulkhead Rupture"),
        FlightWaypoint("18:25:18", 139.080, 34.780, 23900.0, 310, "Overflying Izu Peninsula; pilot squawks 7700 emergency", isCritical = true, tag = "Squawk 7700"),
        FlightWaypoint("18:27:07", 138.830, 34.700, 24400.0, 280, "Uncommanded right bank into Suruga Bay corridor", isCritical = false, tag = "Suruga Turn"),
        FlightWaypoint("18:28:36", 138.600, 34.710, 22100.0, 280, "Phugoid oscillation onset across Suruga Bay", isCritical = true, tag = "Phugoid Onset"),
        FlightWaypoint("18:31:08", 138.380, 34.800, 24900.0, 250, "Deep turn near Yaizu; peak altitude 24,900 ft reached", isCritical = true, tag = "Peak 24,900 ft"),
        FlightWaypoint("18:34:53", 138.450, 35.150, 21400.0, 270, "Heading north toward Yamanashi / Mt. Fuji corridor", isCritical = false, tag = "Northbound"),
        FlightWaypoint("18:38:08", 138.820, 35.400, 22400.0, 260, "Northwest of Mt. Fuji; asymmetric thrust steering", isCritical = false, tag = "Mt. Fuji Pass"),
        FlightWaypoint("18:40:30", 138.980, 35.580, 22400.0, 220, "Approaching Otsuki; entering right-hand 360° orbit", isCritical = true, tag = "Otsuki Orbit Start"),
        FlightWaypoint("18:41:59", 139.120, 35.610, 20900.0, 240, "Completing east curve of Otsuki orbit", isCritical = false, tag = "Otsuki East Curve"),
        FlightWaypoint("18:43:05", 139.060, 35.520, 18600.0, 240, "Tight turn southeast of Otsuki; rapid altitude drop", isCritical = false, tag = "Otsuki Descent"),
        FlightWaypoint("18:44:09", 139.000, 35.680, 17000.0, 240, "Exiting 360° Otsuki orbit heading northward", isCritical = true, tag = "Otsuki Exit"),
        FlightWaypoint("18:45:48", 139.180, 35.700, 13500.0, 220, "Steep dive towards Yokota Air Base vicinity", isCritical = false, tag = "Yokota Sector"),
        FlightWaypoint("18:47:17", 139.260, 35.760, 9000.0, 230, "West of Yokota; gear extended to stabilize pitch", isCritical = true, tag = "Gear Extended"),
        FlightWaypoint("18:48:03", 139.250, 35.880, 6800.0, 230, "Lowest altitude reached (6,800 ft); full thrust applied", isCritical = true, tag = "Lowest 6,800 ft"),
        FlightWaypoint("18:51:03", 139.050, 36.010, 9600.0, 190, "Climbing through mountains near Mt. Mikuni", isCritical = false, tag = "Mt. Mikuni"),
        FlightWaypoint("18:53:03", 138.860, 36.070, 13400.0, 180, "Climbing north toward Nagano / Gunma prefecture border", isCritical = false, tag = "Gunma Ridge"),
        FlightWaypoint("18:54:25", 138.740, 36.060, 11000.0, 220, "Flaps extended; uncontrollable right roll begins", isCritical = true, tag = "Flaps & Severe Roll"),
        FlightWaypoint("18:55:15", 138.680, 36.030, 8400.0, 250, "Steep right bank descent into mountain ridges", isCritical = false, tag = "Ridge Descent"),
        FlightWaypoint("18:56:00", 138.690, 36.010, 7200.0, 310, "Ground Proximity Warning System (GPWS) sounds 'PULL UP'", isCritical = true, tag = "GPWS Alarm"),
        FlightWaypoint("18:56:30", 138.693, 35.998, 5135.0, 340, "Final impact at Mount Takamagahara / Osutaka ridge (elevation 1,565m)", isCritical = true, tag = "Final Impact")
    )

    val LANDMARKS: List<FlightLandmark> = listOf(
        FlightLandmark("Tokyo Haneda (HND)", 139.78, 35.55, 6.0, LandmarkType.AIRPORT, "Origin runway 16L; departure at 18:12 JST"),
        FlightLandmark("Mount Fuji", 138.727, 35.360, 3776.0, LandmarkType.MOUNTAIN, "Japan's highest peak, passed northwest at 18:38 JST"),
        FlightLandmark("Otsuki VOR", 138.98, 35.60, 350.0, LandmarkType.VOR_NAV, "Key navigation point where aircraft circled 360°"),
        FlightLandmark("Yokota Air Base", 139.35, 35.75, 141.0, LandmarkType.MILITARY_BASE, "Offered immediate emergency landing on 3,350m runway"),
        FlightLandmark("Sagami Bay", 139.40, 35.10, 0.0, LandmarkType.BAY, "Site of initial aft pressure bulkhead failure at 23,900 ft"),
        FlightLandmark("Suruga Bay", 138.65, 34.90, 0.0, LandmarkType.BAY, "Water corridor where Dutch roll and Phugoid motion developed"),
        FlightLandmark("Mount Takamagahara", 138.693, 35.998, 1979.0, LandmarkType.CRASH_SITE, "Ridge crash location near Mount Osutaka, Ueno Village, Gunma")
    )

    fun parseTimeSeconds(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 3) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val s = parts[2].toIntOrNull() ?: 0
        return h * 3600 + m * 60 + s
    }

    val T0_SECONDS = parseTimeSeconds(WAYPOINTS.first().timeStr)
    val TOTAL_FLIGHT_SECONDS = parseTimeSeconds(WAYPOINTS.last().timeStr) - T0_SECONDS
}
