package com.buglist.presentation.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.buglist.R
import com.buglist.util.KEY_AMBIENT_MOTION
import com.buglist.util.appDataStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map

@Composable
internal fun GoldCharm() {
    val context = LocalContext.current
    val enabled by remember(context) {
        context.appDataStore.data.map { it[KEY_AMBIENT_MOTION] ?: true }
    }.collectAsStateWithLifecycle(initialValue = false)
    if (!enabled) return

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var angle by remember { mutableFloatStateOf(0f) }
    var lift by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!animationsEnabled(context)) return@repeatOnLifecycle
            try {
                animateCharm(context) { rotation, bounce ->
                    angle = rotation.coerceIn(-11f, 11f)
                    lift = bounce.coerceIn(-4f, 4f)
                }
            } finally {
                angle = 0f
                lift = 0f
            }
        }
    }
    Box(Modifier.size(width = 64.dp, height = 52.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.buglist_gold_charm),
            contentDescription = null,
            modifier = Modifier.size(width = 56.dp, height = 28.dp).graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.125f)
                rotationZ = angle
                translationY = lift.dp.toPx()
            }
        )
    }
}

private fun animationsEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    return Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f &&
        Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) > 0f
}

private suspend fun animateCharm(context: Context, draw: (angle: Float, lift: Float) -> Unit) {
    val manager = context.getSystemService(SensorManager::class.java) ?: return
    val orientation = manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val acceleration = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: orientation?.let { manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (orientation == null && acceleration == null) return

    // Registered on the main thread, so the frame loop and sensor callbacks share one target.
    val input = CharmInput()
    val wake = Channel<Unit>(Channel.CONFLATED)
    val matrix = FloatArray(9)
    var hasOrientation = false
    var target = CharmPose()
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            if (values.size < 3 || !values[0].isFinite() || !values[1].isFinite() || !values[2].isFinite()) return
            when (event.sensor.type) {
                Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(matrix, values)
                    input.orientation(matrix[6], matrix[7], matrix[8])
                    hasOrientation = true
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> input.acceleration(values[0], values[1])
                Sensor.TYPE_ACCELEROMETER -> if (hasOrientation) {
                    input.acceleration(
                        values[0] - matrix[6] * SensorManager.GRAVITY_EARTH,
                        values[1] - matrix[7] * SensorManager.GRAVITY_EARTH
                    )
                }
            }
            val next = input.target()
            if (next != target) {
                target = next
                wake.trySend(Unit)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    val rotation = CharmSpring()
    val bounce = CharmSpring()
    try {
        val rotationActive = orientation?.let { manager.registerListener(listener, it, 20_000) } == true
        val accelerationActive = acceleration?.let { manager.registerListener(listener, it, 20_000) } == true
        if (!rotationActive && !accelerationActive) return
        while (true) {
            wake.receive()
            var lastFrame = withFrameNanos { it }
            do {
                val frame = withFrameNanos { it }
                val elapsed = (frame - lastFrame) / 1_000_000_000f
                lastFrame = frame
                val rotating = rotation.advance(target.angle, elapsed)
                val bouncing = bounce.advance(target.lift, elapsed)
                draw(rotation.position, bounce.position)
            } while (rotating || bouncing)
        }
    } finally {
        manager.unregisterListener(listener)
        wake.close()
    }
}
