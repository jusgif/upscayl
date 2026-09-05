package com.jusgif.upscayl

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelManager(private val context: Context) {
    companion object {
        private const val BASE = "https://huggingface.co/aigchacker/realesrgan-x4/resolve/main/"
        private const val PARAM = "realesrgan-x4plus.param"
        private const val BIN = "realesrgan-x4plus.bin"
    }
    private val dir get() = File(context.filesDir, "models/realesrgan-x4plus")
    fun isReady() = File(dir, PARAM).length() > 1000 && File(dir, BIN).length() > 1_000_000
    fun path(): String = dir.absolutePath
    fun ensureReady(onProgress: (Int) -> Unit) {
        if (isReady()) { onProgress(100); return }
        dir.mkdirs()
        download(PARAM, onProgress, 0, 10)
        download(BIN, onProgress, 10, 90)
        onProgress(100)
    }
    private fun download(name: String, onProgress: (Int) -> Unit, start: Int, span: Int) {
        val out = File(dir, name)
        val c = (URL(BASE + name).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
        }
        c.connect()
        if (c.responseCode !in 200..299) throw IllegalStateException("Model download failed: ${c.responseCode}")
        val total = c.contentLengthLong
        c.inputStream.use { input -> out.outputStream().use { output ->
            val buf = ByteArray(128 * 1024); var readTotal = 0L; var n: Int
            while (input.read(buf).also { n = it } >= 0) {
                if (n == 0) continue
                output.write(buf, 0, n)
                readTotal += n
                if (total > 0) onProgress(start + ((readTotal * span) / total).toInt())
            }
        }}
        c.disconnect()
    }
}
