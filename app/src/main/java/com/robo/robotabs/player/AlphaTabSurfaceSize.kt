package com.robo.robotabs.player

import alphaTab.AlphaTabView
import android.view.View
import net.alphatab.R

// alphaTab 1.9.0 sizes its Android surface from partial results only, so the last system is cut off.
private val surfaceClass = Class.forName("alphaTab.platform.android.AlphaTabRenderSurface")
private val totalWidthField = surfaceClass.getDeclaredField("_totalWidth").apply { isAccessible = true }
private val totalHeightField = surfaceClass.getDeclaredField("_totalHeight").apply { isAccessible = true }

fun AlphaTabView.applyFinalSizeOnRenderFinished() {
    val surface = findViewById<View>(R.id.renderSurface)
    api.renderFinished.on { result ->
        surface.post {
            totalWidthField.setInt(surface, result.totalWidth.toInt())
            totalHeightField.setInt(surface, result.totalHeight.toInt())
            surface.requestLayout()
        }
    }
}
