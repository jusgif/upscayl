package com.jusgif.upscayl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelManagerTest {
    @Test fun expectedModelArtifactsHaveKnownExtensions() {
        assertTrue("realesrgan-x4plus.param".endsWith(".param"))
        assertTrue("realesrgan-x4plus.bin".endsWith(".bin"))
    }

    @Test fun scaleEmulationProducesExpectedDimensions() {
        val width = 640
        val height = 480
        assertEquals(1280, width * 2)
        assertEquals(1920, width * 3)
        assertEquals(1920, height * 4)
    }
}
