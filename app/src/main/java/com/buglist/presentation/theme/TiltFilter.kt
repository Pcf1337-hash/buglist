package com.buglist.presentation.theme

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.exp

/**
 * Turns a gravity-free rotation matrix into degrees.
 * Pitch is rotation about x, roll about y, both in degrees.
 * The matrix is the one Android returns after remapping into display axes.
 */
fun pitchRollDegrees(rotationMatrix: FloatArray): Pair<Float, Float> {
    require(rotationMatrix.size == 9)
    val pitch = asin((-rotationMatrix[7]).coerceIn(-1f, 1f))
    val roll = atan2(-rotationMatrix[6], rotationMatrix[8])
    return Math.toDegrees(pitch.toDouble()).toFloat() to Math.toDegrees(roll.toDouble()).toFloat()
}

data class TiltOffset(val xDp: Float, val yDp: Float)

/**
 * Smooths tilt into a few dp of decoration.
 * Deadzone, clamp and the 8 dp cap run before the low-pass so a mount
 * vibration under one degree never enters the filter.
 */
class TiltFilter(
    private val deadzoneDeg: Float = 1f,
    private val clampDeg: Float = 12f,
    private val maxDp: Float = 8f,
    private val timeConstantMs: Float = 275f
) {
    private var filteredPitch = 0f
    private var filteredRoll = 0f
    private var primed = false

    fun reset() {
        primed = false
    }

    fun step(
        pitchDeg: Float,
        rollDeg: Float,
        neutralPitch: Float,
        neutralRoll: Float,
        dtMs: Float
    ): TiltOffset {
        val pitch = (pitchDeg - neutralPitch).let { if (abs(it) < deadzoneDeg) 0f else it }
            .coerceIn(-clampDeg, clampDeg)
        val roll = (rollDeg - neutralRoll).let { if (abs(it) < deadzoneDeg) 0f else it }
            .coerceIn(-clampDeg, clampDeg)
        if (!primed) {
            filteredPitch = pitch
            filteredRoll = roll
            primed = true
        } else {
            val alpha = 1f - exp(-(dtMs.coerceAtLeast(1f) / timeConstantMs))
            filteredPitch += alpha * (pitch - filteredPitch)
            filteredRoll += alpha * (roll - filteredRoll)
        }
        return TiltOffset(
            xDp = filteredRoll / clampDeg * maxDp,
            yDp = filteredPitch / clampDeg * maxDp
        )
    }
}
