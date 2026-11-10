package com.buglist.presentation.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.buglist.R
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun GoldCharm(width: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var angle by remember { mutableFloatStateOf(0f) }
    var lift by remember { mutableFloatStateOf(0f) }
    var twist by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!animationsEnabled(context)) return@repeatOnLifecycle
            try {
                animateCharm(context) { rotation, bounce, turn ->
                    angle = rotation.coerceIn(-11f, 11f)
                    lift = bounce.coerceIn(-4f, 4f)
                    twist = turn.coerceIn(-11f, 11f)
                }
            } finally {
                angle = 0f
                lift = 0f
                twist = 0f
            }
        }
    }
    val artworkWidth = width - 16.dp
    val artworkHeight = artworkWidth * (224f / 400f)
    val hangOffset = 14.dp
    val chainHeight = hangOffset + artworkHeight * 0.17f
    val atlas = ImageBitmap.imageResource(R.drawable.buglist_gold_charm_atlas)
    val frameSize = IntSize(atlas.width / 5, atlas.height / 7)
    // As the chain tilts towards the viewer its projection shortens; it never stretches.
    fun projectedLift() = abs(lift) * 0.25f + (twist * twist / 121f) * 0.9f
    Box(
        modifier.size(width = width, height = hangOffset + artworkHeight + 16.dp).clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(width = artworkWidth, height = hangOffset + artworkHeight).graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = angle * 1.35f
            }
        ) {
            Image(
                painter = painterResource(R.drawable.buglist_gold_chain),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.align(Alignment.TopCenter).size(8.dp, chainHeight).graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleY = 1f - projectedLift() / chainHeight.value
                }
            )
            Canvas(
                Modifier.align(Alignment.BottomCenter)
                    .size(artworkWidth, artworkHeight)
                    .preferredFrameRate(60f)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0.17f)
                        translationY = -projectedLift().dp.toPx()
                        rotationZ = (0.32f * (twist - angle)).coerceIn(-4.5f, 4.5f)
                    }
            ) {
                // The atlas stores 35 evenly spaced views of the same model, from left to right.
                val frame = (17f + 17f * twist / 11f).roundToInt()
                drawImage(
                    image = atlas,
                    srcOffset = IntOffset(frame % 5 * frameSize.width, frame / 5 * frameSize.height),
                    srcSize = frameSize,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            }
        }
    }
}

private fun animationsEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    return Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f &&
        Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) > 0f
}

private suspend fun animateCharm(context: Context, draw: (angle: Float, lift: Float, twist: Float) -> Unit) {
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
    // The pendant turns around its own eyelet more slowly than the chain swings.
    val twist = CharmSpring(stiffness = 78f, damping = 11f)
    try {
        val rotationActive = orientation?.let { manager.registerListener(listener, it, 20_000) } == true
        val accelerationActive = acceleration?.let { manager.registerListener(listener, it, 20_000) } == true
        if (!rotationActive && !accelerationActive) return
        while (true) {
            wake.receive()
            var lastFrame = withFrameNanos { it }
            do {
                val moving = withFrameNanos { frame ->
                    val elapsed = (frame - lastFrame) / 1_000_000_000f
                    lastFrame = frame
                    val rotating = rotation.advance(target.angle, elapsed)
                    val bouncing = bounce.advance(target.lift, elapsed)
                    val turning = twist.advance((target.angle + 0.6f * target.lift).coerceIn(-11f, 11f), elapsed)
                    draw(rotation.position, bounce.position, twist.position)
                    rotating || bouncing || turning
                }
            } while (moving)
        }
    } finally {
        manager.unregisterListener(listener)
        wake.close()
    }
}
