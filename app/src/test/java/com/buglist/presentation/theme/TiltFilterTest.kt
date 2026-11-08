package com.buglist.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class TiltFilterTest {
    @Test
    fun identityMatrixIsLevel() {
        val matrix = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        val (pitch, roll) = pitchRollDegrees(matrix)
        assertEquals(0f, pitch, 0.01f)
        assertEquals(0f, roll, 0.01f)
    }

    @Test
    fun deadzoneSwallowsASmallTilt() {
        val filter = TiltFilter()
        val offset = filter.step(0.4f, -0.6f, 0f, 0f, dtMs = 5_000f)
        assertEquals(0f, offset.xDp, 0.001f)
        assertEquals(0f, offset.yDp, 0.001f)
    }

    @Test
    fun twelveDegreesMapsToEightDpAndFurtherTiltDoesNot() {
        val filter = TiltFilter()
        val offset = filter.step(40f, -30f, 0f, 0f, dtMs = 5_000f)
        assertEquals(-8f, offset.xDp, 0.05f)
        assertEquals(8f, offset.yDp, 0.05f)
    }

    @Test
    fun neutralPointCancelsThePoseHeldAtResume() {
        val filter = TiltFilter()
        val offset = filter.step(pitchDeg = 8f, rollDeg = -4f, neutralPitch = 8f, neutralRoll = -4f, dtMs = 5_000f)
        assertEquals(0f, offset.xDp, 0.001f)
        assertEquals(0f, offset.yDp, 0.001f)
    }

    @Test
    fun lowPassDoesNotJumpOnTheFirstShortSample() {
        val filter = TiltFilter()
        filter.step(0f, 0f, 0f, 0f, dtMs = 16f)
        val offset = filter.step(12f, 0f, 0f, 0f, dtMs = 16f)
        assertEquals(true, abs(offset.yDp) < 2f)
    }
}
