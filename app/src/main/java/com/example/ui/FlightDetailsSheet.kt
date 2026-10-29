package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FlightData
import com.example.data.KmlExporter
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsSheet(
    onDismiss: () -> Unit,
    onSelectWaypoint: (Int) -> Unit,
    currentWaypointIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CockpitSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) },
        modifier = modifier.testTag("flight_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JAL Flight 123 Investigation Dossier",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Boeing 747-SR46 • Registration JA8119",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { KmlExporter.shareTelemetrySummary(context) },
                        modifier = Modifier.testTag("share_telemetry_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Telemetry",
                            tint = HudCyan
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sheet_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Tab bar
            val tabs = listOf("Chronology", "Radar Telemetry", "Aircraft Systems")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CockpitSurface,
                contentColor = HudCyan,
                divider = { HorizontalDivider(color = Color(0xFF1E293B)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tab Content
            when (selectedTab) {
                0 -> ChronologyTab(onSelectWaypoint)
                1 -> RadarTelemetryTab(onSelectWaypoint, currentWaypointIndex)
                2 -> AircraftSystemsTab()
            }
        }
    }
}

@Composable
private fun ChronologyTab(onSelectWaypoint: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Surface(
                color = HudRedJal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HudRedJal.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "18:24:35 JST • CATASTROPHIC EVENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudRed
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "12 minutes after departure from Haneda Airport, at 23,900 ft over Sagami Bay near Izu Oshima, the aft pressure bulkhead failed explosively. The decompression tore off 5 meters of the vertical stabilizer and severed all 4 redundant hydraulic lines (A, B, C, D), causing immediate, total loss of flight control surfaces.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 17.sp)
                    )
                }
            }
        }

        item {
            Surface(
                color = HudAmber.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HudAmber.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "18:25 - 18:40 JST • DUTCH ROLL & PHUGOID STRUGGLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudAmber
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "With ailerons, elevators, and rudder completely inoperative, Captain Masami Takahama, First Officer Yutaka Sasaki, and Flight Engineer Hiroshi Fukuda steered the 747 solely via differential thrust of the 4 engines and landing gear extension. The aircraft cycled in severe Dutch roll yaw oscillations and Phugoid altitude swings between 20,000 ft and 25,000 ft.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 17.sp)
                    )
                }
            }
        }

        item {
            Surface(
                color = HudCyan.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "18:40 - 18:56 JST • OTSUKI 360° ORBIT & TERRAIN DESCENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudCyan
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Over Otsuki, the crew executed a full clockwise circle while descending from 22,400 ft to 17,000 ft, then plunged down into the mountains towards Yokota Air Base (dropping to 6,800 ft). Despite superhuman 32-minute airmanship, the plane entered an uncontrollable right bank in the mountains of Gunma prefecture and impacted the ridge of Mount Takamagahara at 18:56:30.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 17.sp)
                    )
                }
            }
        }

        item {
            Surface(
                color = Color(0xFF131D33),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CAUSE OF FAILURE (OFFICIAL JTSB INQUIRY)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudEmerald
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "7 years prior (June 1978), JA8119 suffered a tailstrike landing at Osaka Itami Airport. Boeing repair technicians incorrectly installed a two-piece doubler plate with a single row of rivets instead of a continuous splice plate with double rows, reducing fatigue strength by 70%. After 12,319 takeoffs and landings, the splice failed under cabin pressure.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 17.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarTelemetryTab(
    onSelectWaypoint: (Int) -> Unit,
    currentWaypointIndex: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(FlightData.WAYPOINTS) { index, wp ->
            val isCurrent = (index == currentWaypointIndex)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectWaypoint(index) },
                color = if (isCurrent) HudCyan.copy(alpha = 0.15f) else Color(0x330F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isCurrent) HudCyan.copy(alpha = 0.6f) else Color(0x2238BDF8)
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = wp.timeStr,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (wp.isCritical) HudRed else HudCyan
                                )
                            )
                            if (wp.tag.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = wp.tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = wp.event,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Lat: %.2f°N  Lon: %.2f°E".format(wp.lat, wp.lon),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%,d ft".format(wp.altFt.toInt()),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = getAltitudeColor(wp.altFt)
                            )
                        )
                        Text(
                            text = "${wp.speedKt} kt",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AircraftSystemsTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Surface(
                color = Color(0x330F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AIRCRAFT SPECIFICATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudCyan
                        )
                    )
                    SpecRow("Aircraft Model", "Boeing 747-SR46 (Short Range Domestic)")
                    SpecRow("Registration", "JA8119 (Delivered January 1974)")
                    SpecRow("Serial Number", "20783 / Line No. 230")
                    SpecRow("Engines", "4× Pratt & Whitney JT9D-7A turbofans")
                    SpecRow("Flight Hours / Cycles", "12,319 hours / 18,835 cycles")
                    SpecRow("Maximum Seating", "528 passenger configuration")
                }
            }
        }

        item {
            Surface(
                color = Color(0x330F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUADRUPLE HYDRAULIC SYSTEM FAILURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudRed
                        )
                    )
                    Text(
                        text = "The Boeing 747 was designed with 4 fully independent hydraulic systems (Systems 1, 2, 3, 4). However, all four lines converged physically in the tail cone adjacent to the aft pressure bulkhead. When the bulkhead failed, pressurized cabin air severed all 4 lines simultaneously, dropping control pressure from 3,000 psi to 0 psi within 90 seconds.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 16.sp)
                    )
                }
            }
        }

        item {
            Surface(
                color = Color(0x330F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SURVIVORS & RESCUE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = HudEmerald
                        )
                    )
                    Text(
                        text = "Four female passengers seated in rows 54 to 60 near the rear left fuselage miraculously survived: Yumi Ochiai (26, off-duty JAL flight attendant), Hiroko Yoshizaki (34) and her daughter Mikiko (8), and Keiko Kawakami (12). Their survival led to worldwide aviation safety reforms in rear fuselage survivability and hydraulic shutoff valves.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 16.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        )
    }
}
