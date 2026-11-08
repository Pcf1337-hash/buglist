package com.buglist.presentation.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.view.Surface
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Shared pause counter. Tilgen acquires one hold and releases it on dispose. */
class AmbientGate {
    val holds = MutableStateFlow(0)
    fun acquire() { holds.update { it + 1 } }
    fun release() { holds.update { (it - 1).coerceAtLeast(0) } }
}

val AmbientGates = AmbientGate()

val LocalAmbientShift = staticCompositionLocalOf { AmbientShift() }

class AmbientShift {
    var xDp by mutableFloatStateOf(0f)
    var yDp by mutableFloatStateOf(0f)
}

fun reducedMotion(context: Context): Boolean {
    val resolver = context.contentResolver
    val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return animator == 0f || transition == 0f
}

/**
 * Listens only while resumed, enabled and not paused. Writes the shift on the
 * main thread. Missing sensor or reduced motion leaves the shift at zero.
 */
@Composable
fun TrackAmbientTilt(shift: AmbientShift, enabled: Boolean, paused: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, enabled, paused) {
        if (!enabled || paused || reducedMotion(context)) {
            shift.xDp = 0f
            shift.yDp = 0f
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        if (manager == null || sensor == null) {
            shift.xDp = 0f
            shift.yDp = 0f
            return@DisposableEffect onDispose { }
        }
        val listener = TiltListener(manager, view, shift)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    listener.recenter()
                    manager.registerListener(listener, sensor, 50_000)
                }
                Lifecycle.Event.ON_PAUSE -> manager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            listener.recenter()
            manager.registerListener(listener, sensor, 50_000)
        }
        onDispose {
            manager.unregisterListener(listener)
            lifecycle.removeObserver(observer)
            shift.xDp = 0f
            shift.yDp = 0f
        }
    }
}

private class TiltListener(
    private val manager: SensorManager,
    private val view: View,
    private val shift: AmbientShift
) : SensorEventListener {
    private val filter = TiltFilter()
    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private var neutralPitch = 0f
    private var neutralRoll = 0f
    private var haveNeutral = false
    private var lastRotation = Int.MIN_VALUE
    private var lastTimestampNs = 0L

    fun recenter() {
        haveNeutral = false
        lastTimestampNs = 0L
        filter.reset()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.isEmpty()) return
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        val turn = view.display?.rotation ?: Surface.ROTATION_0
        val (xAxis, yAxis) = axesFor(turn)
        val ok = SensorManager.remapCoordinateSystem(rotation, xAxis, yAxis, remapped)
        val (pitch, roll) = pitchRollDegrees(if (ok) remapped else rotation)
        if (!haveNeutral || turn != lastRotation) {
            neutralPitch = pitch
            neutralRoll = roll
            lastRotation = turn
            haveNeutral = true
            filter.reset()
            lastTimestampNs = event.timestamp
            view.post {
                shift.xDp = 0f
                shift.yDp = 0f
            }
            return
        }
        val dtMs = if (lastTimestampNs == 0L) 16f else ((event.timestamp - lastTimestampNs) / 1_000_000f).coerceIn(1f, 250f)
        lastTimestampNs = event.timestamp
        val offset = filter.step(pitch, roll, neutralPitch, neutralRoll, dtMs)
        view.post {
            shift.xDp = offset.xDp
            shift.yDp = offset.yDp
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun axesFor(rotation: Int): Pair<Int, Int> = when (rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
}

/** A short highlight on the existing edge. The card itself does not move. */
fun Modifier.ambientRim(shift: AmbientShift, corner: Dp = 20.dp): Modifier = drawWithContent {
    drawContent()
    val dx = (shift.xDp / 8f).coerceIn(-1f, 1f)
    val dy = (shift.yDp / 8f).coerceIn(-1f, 1f)
    val stroke = 1.5.dp.toPx()
    val start = Offset(size.width * (0.15f + 0.35f * dx), size.height * (0.05f + 0.2f * dy))
    val end = Offset(size.width * (0.85f - 0.2f * dx), size.height * (0.95f - 0.15f * dy))
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                BugListColors.Gold.copy(alpha = 0.05f),
                BugListColors.Gold.copy(alpha = 0.55f),
                BugListColors.Gold.copy(alpha = 0.05f)
            ),
            start = start,
            end = end
        ),
        style = Stroke(width = stroke),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx(), corner.toPx())
    )
}
