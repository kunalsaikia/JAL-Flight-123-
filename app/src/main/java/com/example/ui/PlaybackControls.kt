package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FlightData
import com.example.data.InterpolatedFlightState
import com.example.ui.theme.*

@Composable
fun PlaybackControlsBar(
    isPlaying: Boolean,
    playbackSpeed: Float,
    flightState: InterpolatedFlightState,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    onSeekProgress: (Float) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onJumpToWaypoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("playback_controls_bar"),
        color = CockpitGlass,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, Color(0x3338BDF8)),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Time Scrubber Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "18:13:55",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                )

                Slider(
                    value = flightState.progress,
                    onValueChange = { onSeekProgress(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("flight_time_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = if (flightState.isDecompressed) HudRed else HudCyan,
                        activeTrackColor = if (flightState.isDecompressed) HudRed else HudCyan,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Text(
                    text = "18:56:30",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = HudRose
                    )
                )
            }

            // 2. Playback Action Buttons & Speed Multipliers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause / Reset
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("play_pause_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = HudCyan,
                            contentColor = Color(0xFF030712)
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OutlinedIconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("reset_button"),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateLeft,
                            contentDescription = "Reset Flight",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Speed Multiplier Chips
                    Surface(
                        color = Color(0x660F172A),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0x2238BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1f, 3f, 6f, 12f, 24f).forEach { spd ->
                                val isSelected = (playbackSpeed == spd)
                                Surface(
                                    onClick = { onSelectSpeed(spd) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) HudCyan.copy(alpha = 0.25f) else Color.Transparent,
                                    modifier = Modifier.padding(horizontal = 1.dp)
                                ) {
                                    Text(
                                        text = "${spd.toInt()}x",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSelected) HudCyan else TextSecondary
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Mini Altitude & Speed indicator on right
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%,d ft".format(flightState.altFt.toInt()),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = getAltitudeColor(flightState.altFt)
                        )
                    )
                    Text(
                        text = "${flightState.speedKt} kt",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = HudCyan
                        )
                    )
                }
            }

            // 3. Quick Landmark Jump Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JUMP:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                )

                val jumpMilestones = listOf(
                    0 to "Haneda Departure",
                    4 to "18:24 Decompression",
                    7 to "Suruga Bay",
                    11 to "Otsuki 360°",
                    17 to "Lowest 6,800ft",
                    22 to "GPWS Pull Up",
                    23 to "Crash Ridge"
                )

                jumpMilestones.forEach { (index, label) ->
                    val isCurrent = (flightState.waypointIndex == index)
                    val chipColor = when (index) {
                        4 -> HudRed
                        11 -> HudAmber
                        23 -> HudPurple
                        else -> HudCyan
                    }

                    FilterChip(
                        selected = isCurrent,
                        onClick = { onJumpToWaypoint(index) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.25f),
                            selectedLabelColor = chipColor,
                            containerColor = Color(0x330F172A),
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isCurrent,
                            borderColor = Color(0x2238BDF8),
                            selectedBorderColor = chipColor.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}
