package com.robo.robotabs.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphaTabSurfaceSizeTest {
    @Test
    fun privateSizeFieldsStillExistInAlphaTab() {
        val surface = Class.forName("alphaTab.platform.android.AlphaTabRenderSurface")

        assertEquals(Int::class.javaPrimitiveType, surface.getDeclaredField("_totalWidth").type)
        assertEquals(Int::class.javaPrimitiveType, surface.getDeclaredField("_totalHeight").type)
    }
}
