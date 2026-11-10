package com.buglist.presentation.dashboard

import kotlin.math.abs
import kotlin.math.sign

internal data class CharmPose(val angle: Float = 0f, val lift: Float = 0f)

/** The third row of the rotation matrix is world up in phone coordinates. */
internal class CharmInput {
    private var neutralX = 0f
    private var neutralZ = 0f
    private var calibrated = false
    private var upright = false
    private var tiltX = 0f
    private var tiltZ = 0f
    private var lateral = 0f
    private var vertical = 0f

    fun orientation(upX: Float, upY: Float, upZ: Float) {
        // Hysteresis avoids switching sensitivity back and forth at the threshold.
        val nowUpright = upY > if (upright) 0.65f else 0.75f
        if (!calibrated || nowUpright != upright) {
            neutralX = upX
            neutralZ = upZ
            calibrated = true
        }
        upright = nowUpright
        tiltX = upX - neutralX
        tiltZ = upZ - neutralZ
    }

    fun acceleration(x: Float, y: Float) {
        lateral = x
        vertical = y
    }

    fun target(): CharmPose {
        val tiltThreshold = if (upright) 0.004f else 0.018f
        val accelerationThreshold = if (upright) 0.16f else 0.45f
        val gain = if (upright) 1f else 0.45f
        val angle = 150f * deadband(tiltX, tiltThreshold) -
            6f * gain * deadband(lateral, accelerationThreshold)
        val lift = 60f * deadband(tiltZ, tiltThreshold) +
            2.5f * gain * deadband(vertical, accelerationThreshold)
        return CharmPose(angle.coerceIn(-10f, 10f), lift.coerceIn(-3.5f, 3.5f))
    }

    private fun deadband(value: Float, threshold: Float): Float =
        (abs(value) - threshold).coerceAtLeast(0f) * sign(value)
}

/** A damped spring with short integration steps, independent of sensor and display rates. */
internal class CharmSpring(
    private val stiffness: Float = 140f,
    private val damping: Float = 15f
) {
    var position = 0f
        private set
    private var velocity = 0f

    fun advance(target: Float, seconds: Float): Boolean {
        var remaining = seconds.coerceIn(0f, 0.05f)
        while (remaining > 0f) {
            val step = minOf(remaining, 1f / 120f)
            velocity += (stiffness * (target - position) - damping * velocity) * step
            position += velocity * step
            remaining -= step
        }
        if (abs(target - position) < 0.015f && abs(velocity) < 0.08f) {
            position = target
            velocity = 0f
            return false
        }
        return true
    }
}
