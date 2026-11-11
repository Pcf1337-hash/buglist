package com.buglist.presentation.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharmMotionTest {
    @Test fun uprightMountStartsStillAndRespondsToSmallTiltWithoutEulerAngleJump() {
        val input = CharmInput()
        input.orientation(0f, 0.99995f, 0.01f)
        assertEquals(CharmPose(), input.target())

        // Crossing exactly vertical in the mount must not flip the artwork.
        input.orientation(0f, 0.99995f, -0.01f)
        assertEquals(0f, input.target().angle, 0.001f)
        assertTrue(input.target().lift in -1.1f..-0.8f)
    }

    @Test fun smallBumpReactsInUprightMountWhileVibrationBelowThresholdIsIgnored() {
        val upright = CharmInput().apply { orientation(0f, 1f, 0f) }
        val flat = CharmInput().apply { orientation(0f, 0f, 1f) }
        upright.acceleration(0.07f, -0.08f)
        assertEquals(CharmPose(), upright.target())

        upright.acceleration(0.3f, 0.4f)
        flat.acceleration(0.3f, 0.4f)
        assertTrue(upright.target().angle < -0.5f)
        assertTrue(upright.target().lift > 0.5f)
        assertEquals(CharmPose(), flat.target())
    }

    @Test fun movingFromFlatIntoTheMountRecalibratesTheRestPosition() {
        val input = CharmInput()
        input.orientation(0f, 0f, 1f)
        input.orientation(0.06f, 0.99f, 0.1f)
        assertEquals(CharmPose(), input.target())

        input.orientation(0.05f, 0.99f, 0.12f)
        assertTrue(input.target().angle < 0f)
        assertTrue(input.target().lift > 0f)
    }

    @Test fun springInterpolatesAnImpulseAndReturnsToRestAtEitherRefreshRate() {
        for (refreshRate in listOf(60, 120)) {
            val spring = CharmSpring()
            val frameSeconds = 1f / refreshRate
            assertTrue(spring.advance(3.5f, frameSeconds))
            assertTrue(spring.position in 0.001f..1f)
            repeat(refreshRate / 10) { spring.advance(3.5f, frameSeconds) }
            assertTrue(spring.position > 1f)
            repeat(refreshRate) { spring.advance(0f, frameSeconds) }
            assertEquals(0f, spring.position, 0.001f)
        }
    }

    @Test fun sidewaysBumpsSwingTheChainWhileOnlyTurningThePhoneTwistsTheCharm() {
        val input = CharmInput().apply { orientation(0f, 1f, 0f) }
        input.acceleration(1.2f, 0.9f)
        assertTrue(input.target().angle < 0f)
        assertEquals(0f, input.target().twist, 0.001f)

        input.turn(1f)
        assertTrue(input.target().twist < -10f)
        input.turn(-4f)
        assertEquals(28f, input.target().twist, 0.001f)
        input.turn(-0.02f)
        assertEquals(0f, input.target().twist, 0.001f)
    }
}
