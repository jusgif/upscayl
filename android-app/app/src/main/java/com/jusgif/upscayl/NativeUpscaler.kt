package com.jusgif.upscayl

import android.graphics.Bitmap

object NativeUpscaler {
    init { System.loadLibrary("upscayl_native") }
    external fun upscale(bitmap: Bitmap, modelDir: String, tileSize: Int, useVulkan: Boolean): Bitmap
}
