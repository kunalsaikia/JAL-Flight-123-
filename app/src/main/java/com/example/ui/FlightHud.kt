package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraPerspective
import com.example.data.FlightData
import com.example.data.InterpolatedFlightState
import com.example.data.TerrainTextureMode
import com.example.ui.theme.*

@Composable
fun FlightHeader(
    currentPerspective: CameraPerspective,
    terrainMode: TerrainTextureMode,
    followAircraft: Boolean,
    onSelectPerspective: (CameraPerspective) -> Unit,
    onToggleTerrainMode: () -> Unit,
    onCenterAircraft: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flight Identity Card
        Surface(
            color = CockpitGlass,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x3338BDF8)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .clickable { onOpenInfo() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HudRedJal.copy(alpha = 0.2f))
                        .border(1.dp, HudRedJal.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Flight 123",
                        tint = HudRed,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(-45f)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "JAL 123",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HudRed
                            )
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "B747-SR46",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = HudCyan
                                )
                            )
                        }
                    }
                    Text(
                        text = "HND → ITM • 12 AUG 1985",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        }

        // Camera Toolbar
        Surface(
            color = CockpitGlass,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x3338BDF8)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CameraIconButton(
                    icon = Icons.Default.Camera,
                    label = "3D",
                    isSelected = currentPerspective == CameraPerspective.FREE_ORBIT,
                    onClick = { onSelectPerspective(CameraPerspective.FREE_ORBIT) },
                    testTag = "cam_free_btn"
                )
                CameraIconButton(
                    icon = Icons.Default.Navigation,
                    label = "Chase",
                    isSelected = currentPerspective == CameraPerspective.CHASE_B747,
                    onClick = { onSelectPerspective(CameraPerspective.CHASE_B747) },
                    testTag = "cam_chase_btn"
                )
                CameraIconButton(
                    icon = Icons.Default.Map,
                    label = "Radar",
                    isSelected = currentPerspective == CameraPerspective.TACTICAL_MAP,
                    onClick = { onSelectPerspective(CameraPerspective.TACTICAL_MAP) },
                    testTag = "cam_map_btn"
                )
                CameraIconButton(
                    icon = Icons.Default.ShowChart,
                    label = "Profile",
                    isSelected = currentPerspective == CameraPerspective.PROFILE_SIDE,
                    onClick = { onSelectPerspective(CameraPerspective.PROFILE_SIDE) },
                    testTag = "cam_profile_btn"
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(18.dp)
                        .padding(horizontal = 2.dp),
                    color = Color(0x3338BDF8)
                )

                // Terrain Texture Mapping Mode Toggle
                Surface(
                    onClick = onToggleTerrainMode,
                    shape = RoundedCornerShape(8.dp),
                    color = when (terrainMode) {
                        TerrainTextureMode.SATELLITE_COLOR -> HudEmerald.copy(alpha = 0.2f)
                        TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> Color(0xFF64748B).copy(alpha = 0.25f)
                        TerrainTextureMode.TACTICAL_GRID -> Color.Transparent
                    },
                    border = when (terrainMode) {
                        TerrainTextureMode.SATELLITE_COLOR -> BorderStroke(1.dp, HudEmerald.copy(alpha = 0.5f))
                        TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> BorderStroke(1.dp, Color(0xFF94A3B8))
                        TerrainTextureMode.TACTICAL_GRID -> null
                    },
                    modifier = Modifier.testTag("toggle_terrain_texture_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = when (terrainMode) {
                                TerrainTextureMode.SATELLITE_COLOR -> Icons.Default.Public
                                TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> Icons.Default.Terrain
                                TerrainTextureMode.TACTICAL_GRID -> Icons.Default.GridOn
                            },
                            contentDescription = "Toggle Terrain Texture Mode",
                            tint = when (terrainMode) {
                                TerrainTextureMode.SATELLITE_COLOR -> HudEmerald
                                TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> Color(0xFFF1F5F9)
                                TerrainTextureMode.TACTICAL_GRID -> TextSecondary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (terrainMode) {
                                TerrainTextureMode.SATELLITE_COLOR -> "Satellite"
                                TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> "Heightmap"
                                TerrainTextureMode.TACTICAL_GRID -> "Grid"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (terrainMode) {
                                    TerrainTextureMode.SATELLITE_COLOR -> HudEmerald
                                    TerrainTextureMode.GRAYSCALE_HEIGHTMAP -> Color(0xFFF1F5F9)
                                    TerrainTextureMode.TACTICAL_GRID -> TextSecondary
                                }
                            )
                        )
                    }
                }


                IconButton(
                    onClick = onCenterAircraft,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("center_aircraft_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on Boeing 747",
                        tint = if (followAircraft) HudCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Accident Dossier",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) HudCyan.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, HudCyan.copy(alpha = 0.6f)) else null,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) HudCyan else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) HudCyan else TextSecondary
                )
            )
        }
    }
}

/**
 * Tactical Telemetry HUD Card showing live altitude, speed, VSI, and emergency status.
 */
@Composable
fun TelemetryHudCard(
    flightState: InterpolatedFlightState,
    isPfdVisible: Boolean,
    onTogglePfd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Blinking effect for squawk 7700 emergency and GPWS alarm
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .widthIn(max = 330.dp)
            .testTag("telemetry_hud_card"),
        color = CockpitGlass,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            when {
                flightState.isGpwsAlarm -> HudPurple.copy(alpha = pulseAlpha)
                flightState.isDecompressed -> HudRed.copy(alpha = pulseAlpha)
                else -> Color(0x3338BDF8)
            }
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Status Badge + Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                val (badgeText, badgeBg, badgeTextColor) = when {
                    flightState.isGpwsAlarm -> Triple("GPWS TERRAIN ALARM", HudPurple.copy(alpha = 0.3f), HudPurple)
                    flightState.isDecompressed -> Triple("SQUAWK 7700 / LOSS OF CONTROL", HudRed.copy(alpha = 0.3f), HudRed)
                    else -> Triple("NORMAL CLIMB", HudEmerald.copy(alpha = 0.2f), HudEmerald)
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = badgeTextColor
                        )
                    )
                }

                // Primary Flight Instruments toggle button
                Surface(
                    onClick = onTogglePfd,
                    shape = RoundedCornerShape(4.dp),
                    color = if (isPfdVisible) HudCyan.copy(alpha = 0.2f) else Color(0x221E293B),
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Toggle PFD Attitude Indicator",
                        tint = if (isPfdVisible) HudCyan else TextMuted,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(16.dp)
                    )
                }
            }

            // 4-Quadrant Flight Instruments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Clock & Speed
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column {
                        Text(
                            text = "JST CLOCK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                        Text(
                            text = flightState.timeClockStr,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = HudAmber
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "AIRSPEED (IAS)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${flightState.speedKt}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = HudCyan
                                )
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "kt",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Right Column: Altitude & Climb Rate (VSI)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column {
                        Text(
                            text = "PRESSURE ALTITUDE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%,d".format(flightState.altFt.toInt()),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = getAltitudeColor(flightState.altFt)
                                )
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "ft",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                        Text(
                            text = "%,d m".format((flightState.altFt * 0.3048).toInt()),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "VERTICAL SPEED (VSI)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                        val vsPrefix = if (flightState.vsFpm >= 0) "+" else ""
                        Text(
                            text = "$vsPrefix%,d fpm".format(flightState.vsFpm),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (flightState.vsFpm >= 0) HudEmerald else HudRose
                            )
                        )
                    }
                }
            }

            // Artificial Horizon & Attitude Indicator Display
            AnimatedVisibility(visible = isPfdVisible) {
                PrimaryFlightDisplay(
                    pitchDeg = flightState.pitchAngleDeg,
                    bankDeg = flightState.bankAngleDeg,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Current Sector / Event Description
            Surface(
                color = Color(0x330F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x2238BDF8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (flightState.isDecompressed) HudRed else HudCyan)
                    )
                    Text(
                        text = flightState.event,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Miniature Artificial Horizon (Attitude Indicator) showing pitch and bank angles.
 */
@Composable
fun PrimaryFlightDisplay(
    pitchDeg: Double,
    bankDeg: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF030712),
        border = BorderStroke(1.dp, Color(0x44334155))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Horizon Disk
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-bankDeg.toFloat())
            ) {
                // Pitch shift
                val pitchOffsetPercent = (pitchDeg / 30.0).coerceIn(-1.0, 1.0)
                val skyGroundRatio = 0.5f - (pitchOffsetPercent * 0.35f).toFloat()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sky (Blue)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(skyGroundRatio.coerceIn(0.1f, 0.9f))
                            .background(Color(0xFF0284C7))
                    )
                    // Horizon Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White)
                    )
                    // Ground (Brown / Dark)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight((1f - skyGroundRatio).coerceIn(0.1f, 0.9f))
                            .background(Color(0xFF78350F))
                    )
                }
            }

            // Fixed Airplane Reference Crosshair (Yellow)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(16.dp, 3.dp).background(Color(0xFFFACC15)))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFACC15)))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.size(16.dp, 3.dp).background(Color(0xFFFACC15)))
            }

            // Bank angle and pitch degree label overlay
            Text(
                text = "BANK: %+.1f°  PITCH: %+.1f°".format(bankDeg, pitchDeg),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color(0x88000000), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
