package com.buglist.presentation.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

/**
 * Programmatically generated 128×128 film grain texture as a BitmapShader.
 *
 * Draws a tiled noise overlay at 4% alpha with BlendMode.Overlay.
 * This removes the sterile "default-Android" look and adds editorial texture.
 *
 * No PNG asset needed — noise is generated once at first composition.
 */
@Composable
fun rememberGrainBrush(): ShaderBrush {
    return remember {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(seed = 0xDEADBEEF.toInt())
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Random grey value — results in noise texture
                val grey = rng.nextInt(256)
                bitmap.setPixel(x, y, android.graphics.Color.argb(255, grey, grey, grey))
            }
        }
        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        ShaderBrush(shader)
    }
}

/**
 * Applies a subtle grain texture overlay to any Composable.
 *
 * Usage:
 * ```kotlin
 * Box(modifier = Modifier.grainOverlay(grainBrush))
 * ```
 *
 * The grain brush should be obtained via [rememberGrainBrush] at a stable composition site
 * (e.g., root of the theme or screen) to avoid re-creation on every recomposition.
 */
fun Modifier.grainOverlay(brush: ShaderBrush, alpha: Float = 0.04f): Modifier =
    drawWithCache {
        onDrawWithContent {
            drawContent()
            // Draw grain tile with Overlay blend — preserves luminosity, adds texture
            drawRect(
                brush = brush,
                alpha = alpha,
                blendMode = BlendMode.Overlay
            )
        }
    }
