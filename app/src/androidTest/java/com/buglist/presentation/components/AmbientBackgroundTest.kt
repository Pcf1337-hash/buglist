package com.buglist.presentation.components

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.buglist.presentation.theme.BugListAppSurface
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class AmbientBackgroundTest {
    @get:Rule val compose = createComposeRule()

    @Test fun appSurfaceDoesNotCoverTheAmbientBackgroundWithASolidColor() {
        compose.setContent { BugListAppSurface {} }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val upper = pixels[pixels.width / 2, pixels.height / 5]
        val lower = pixels[pixels.width / 2, pixels.height * 4 / 5]
        val difference = abs(upper.red - lower.red) + abs(upper.green - lower.green) + abs(upper.blue - lower.blue)
        assertTrue("The app root must expose the theme gradient rather than an opaque flat surface", difference > 0.02f)
    }
}
