package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FlightData
import com.example.data.TrajectoryMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JAL 123", appName)
    }

    @Test
    fun `flight dataset and trajectory calculations are valid`() {
        assertEquals(24, FlightData.WAYPOINTS.size)
        assertTrue(FlightData.TOTAL_FLIGHT_SECONDS > 2000)

        // Departure state
        val s0 = TrajectoryMath.getFlightState(0.0)
        assertEquals("18:13:55", s0.timeClockStr)
        assertEquals(3200.0, s0.altFt, 1.0)
        assertEquals(false, s0.isDecompressed)

        // Bulkhead rupture state (640s = 18:24:35)
        val sDecomp = TrajectoryMath.getFlightState(640.0)
        assertEquals(true, sDecomp.isDecompressed)

        // Final impact state
        val sFinal = TrajectoryMath.getFlightState(FlightData.TOTAL_FLIGHT_SECONDS.toDouble())
        assertNotNull(sFinal)
        assertEquals(1.0f, sFinal.progress, 0.01f)
    }

    @Test
    fun `terrain elevation mapping produces realistic relief`() {
        // Sagami Bay (sea level)
        val sagamiElev = TrajectoryMath.getTerrainElevation(55.0, 66.0)
        assertEquals(0.0, sagamiElev, 0.01)

        // Mount Fuji peak
        val fujiElev = TrajectoryMath.getTerrainElevation(-82.0, 8.8)
        assertTrue("Mount Fuji should rise prominently", fujiElev > 5.0)

        // Mount Takamagahara / Gunma ridge
        val takaElev = TrajectoryMath.getTerrainElevation(-89.5, -131.5)
        assertTrue("Mount Takamagahara should have mountain elevation", takaElev > 2.5)
    }
}
