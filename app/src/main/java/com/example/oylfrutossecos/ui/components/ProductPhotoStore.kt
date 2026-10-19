package com.example.oylfrutossecos.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object ProductPhotoStore {
    private fun directory(context: Context): File =
        File(context.filesDir, "product_photos").apply { mkdirs() }

    fun create(context: Context): Pair<String, Uri> {
        val file = File(directory(context), "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file.name to uri
    }

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        File(directory(context), File(name).name).delete()
    }

    fun file(context: Context, key: String): File? {
        if (!key.startsWith("photo:")) return null
        val name = key.removePrefix("photo:")
        if (name.isEmpty() || File(name).name != name) return null
        return File(directory(context), name).takeIf { it.isFile }
    }

    fun bitmap(context: Context, key: String, maxSize: Int = 900): android.graphics.Bitmap? {
        val photo = file(context, key) ?: return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photo.absolutePath, options)
        var sample = 1
        while (options.outWidth / sample > maxSize * 2 || options.outHeight / sample > maxSize * 2) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(photo.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
