package com.jioremote.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.OutputStream
import java.util.UUID
import kotlin.math.roundToInt

internal class ChannelLogoStore(private val directory: File) {
    fun importPhoto(resolver: ContentResolver, uri: Uri): String {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val size = info.size
            val scale = minOf(1.0, 512.0 / maxOf(size.width, size.height))
            decoder.setTargetSize(
                (size.width * scale).roundToInt().coerceAtLeast(1),
                (size.height * scale).roundToInt().coerceAtLeast(1),
            )
        }
        try {
            return write { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    internal fun write(encode: (OutputStream) -> Unit): String {
        check(directory.isDirectory || directory.mkdirs())
        val name = "${UUID.randomUUID()}.png"
        val temporary = directory.resolve("$name.tmp")
        try {
            temporary.outputStream().use(encode)
            check(temporary.length() > 0 && temporary.renameTo(directory.resolve(name)))
            return name
        } finally {
            temporary.delete()
        }
    }

    fun delete(name: String) {
        file(name)?.delete()
    }

    fun removeUnused(used: Set<String>) {
        directory.listFiles()?.forEach {
            if (it.name !in used && (filePattern.matches(it.name) || filePattern.matches(it.name.removeSuffix(".tmp")))) {
                it.delete()
            }
        }
    }

    fun file(name: String): File? = name.takeIf(filePattern::matches)?.let(directory::resolve)

    companion object {
        const val directoryName = "channel_logos"
        private val filePattern = Regex("[0-9a-f-]{36}\\.png")
    }
}
