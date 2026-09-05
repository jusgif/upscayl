package com.jusgif.upscayl

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageRepository {
    suspend fun decode(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(BitmapFactory.decodeStream(input)) { "Unable to decode image" }
        }
    }
    suspend fun save(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "upscayl_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Upscayl")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        try {
            context.contentResolver.openOutputStream(uri).use { out ->
                requireNotNull(out)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (t: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw t
        }
        uri
    }
}
