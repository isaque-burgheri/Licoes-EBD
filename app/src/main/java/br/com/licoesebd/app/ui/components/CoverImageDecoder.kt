package br.com.licoesebd.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Decode only enough pixels for a cover, including images already cached on disk. */
internal fun decodeCoverBitmap(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    // Options.inSampleSize defaults to ZERO. Never use it as a divisor before
    // explicitly initializing it; Android only normalizes it inside the decoder.
    var sample = 1
    while (bounds.outWidth / sample > 480 || bounds.outHeight / sample > 800) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.absolutePath, options)
}
