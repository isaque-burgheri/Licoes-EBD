package br.com.licoesebd.app.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverImageDecoderTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun image(width: Int, height: Int): File {
        val file = temporary.newFile()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.RED)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    @Test fun cachedThumbnailDecodesAndCanBeDisplayed() {
        // Regression: 1.6 divided by default inSampleSize (zero) before decoding.
        val file = image(220, 320)
        val decoded = requireNotNull(decodeCoverBitmap(file))
        try {
            assertEquals(220, decoded.width)
            assertEquals(320, decoded.height)
            assertEquals(Color.RED, decoded.getPixel(0, 0))
        } finally {
            decoded.recycle()
        }
    }

    @Test fun largePdfCoverIsDownsampledToBoundedMemory() {
        val decoded = requireNotNull(decodeCoverBitmap(image(1920, 3200)))
        try {
            assertEquals(480, decoded.width)
            assertEquals(800, decoded.height)
            assertEquals(Color.RED, decoded.getPixel(0, 0))
        } finally {
            decoded.recycle()
        }
    }

    @Test fun invalidImageReturnsNull() {
        val file = temporary.newFile().apply { writeText("not an image") }
        assertNull(decodeCoverBitmap(file))
    }
}
