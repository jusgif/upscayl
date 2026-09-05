package com.jusgif.upscayl

import org.junit.Assert.assertTrue
import org.junit.Test

class ModelManagerTest {
    @Test fun expectedModelArtifactsHaveKnownExtensions() {
        assertTrue("realesrgan-x4plus.param".endsWith(".param"))
        assertTrue("realesrgan-x4plus.bin".endsWith(".bin"))
    }
}
