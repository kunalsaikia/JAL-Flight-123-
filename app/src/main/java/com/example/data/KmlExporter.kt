package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object KmlExporter {

    fun generateKmlString(): String {
        val coordLines = StringBuilder()
        FlightData.WAYPOINTS.forEach { wp ->
            val altMeters = "%.1f".format(wp.altFt * 0.3048)
            coordLines.append("${wp.lon},${wp.lat},$altMeters\n")
        }

        val placemarks = StringBuilder()
        FlightData.WAYPOINTS.forEach { wp ->
            val altMeters = "%.1f".format(wp.altFt * 0.3048)
            placemarks.append("""
            <Placemark>
              <name>${wp.timeStr} - ${wp.altFt.toInt()}ft</name>
              <description><![CDATA[<b>Time:</b> ${wp.timeStr} JST<br/><b>Altitude:</b> ${wp.altFt.toInt()} ft<br/><b>Airspeed:</b> ${wp.speedKt} kt<br/><b>Event:</b> ${wp.event}]]></description>
              <Point>
                <altitudeMode>absolute</altitudeMode>
                <coordinates>${wp.lon},${wp.lat},$altMeters</coordinates>
              </Point>
            </Placemark>
            """.trimIndent())
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>JAL Flight 123 Trajectory - 12 August 1985</name>
    <description>Recorded 3D flight trajectory of Japan Air Lines Flight 123 (Boeing 747-SR46, JA8119).</description>
    <Style id="jalLine">
      <LineStyle>
        <color>ff0000ff</color>
        <width>4</width>
      </LineStyle>
      <PolyStyle>
        <color>7f0000ff</color>
      </PolyStyle>
    </Style>
    <Placemark>
      <name>JAL123 3D Flight Path</name>
      <styleUrl>#jalLine</styleUrl>
      <LineString>
        <extrude>1</extrude>
        <tessellate>1</tessellate>
        <altitudeMode>absolute</altitudeMode>
        <coordinates>
$coordLines
        </coordinates>
      </LineString>
    </Placemark>
$placemarks
  </Document>
</kml>"""
    }

    fun shareTelemetrySummary(context: Context) {
        val summary = buildString {
            appendLine("=== JAL FLIGHT 123 TELEMETRY SUMMARY ===")
            appendLine("Aircraft: Boeing 747-SR46 (JA8119)")
            appendLine("Date: 12 August 1985 (18:13:55 - 18:56:30 JST)")
            appendLine("Route: Tokyo Haneda (HND) -> Osaka Itami (ITM)")
            appendLine("Decompression: 18:24:35 JST @ 23,900 ft (Sagami Bay)")
            appendLine("Crash Site: Mount Takamagahara / Osutaka ridge, Gunma")
            appendLine()
            appendLine("Key Milestones:")
            FlightData.WAYPOINTS.filter { it.isCritical }.forEach { wp ->
                appendLine("• ${wp.timeStr} | ${wp.altFt.toInt()} ft | ${wp.speedKt} kt - ${wp.event}")
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_SUBJECT, "JAL Flight 123 Historical Telemetry")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share JAL123 Flight Data")
        context.startActivity(shareIntent)
    }
}
