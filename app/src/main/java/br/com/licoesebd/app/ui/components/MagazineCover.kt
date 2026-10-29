package br.com.licoesebd.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.licoesebd.app.data.model.Magazine
import br.com.licoesebd.app.ui.theme.EbdTypography
import br.com.licoesebd.app.ui.theme.Gold
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val coverGradients = listOf(
    listOf(Color(0xFF16212E), Color(0xFF0A121B)),
    listOf(Color(0xFF4A3818), Color(0xFF2A1F0C)),
    listOf(Color(0xFF2D4A3A), Color(0xFF14241C)),
    listOf(Color(0xFF6B1F24), Color(0xFF3D1014)),
    listOf(Color(0xFF4A2818), Color(0xFF2A160C)),
    listOf(Color(0xFF1F2D4A), Color(0xFF0C1424))
)

/**
 * Compact cover used in the library grid.
 * Loads a cached Drive thumbnail lazily; keeps the colored cover while loading.
 */
@Composable
fun MagazineCover(
    magazine: Magazine,
    coverLoader: suspend (Magazine) -> java.io.File?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bmp = rememberCoverBitmap(magazine, coverLoader)

    val image = bmp
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = magazine.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Subtle bottom badge with quarter
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC0A0A0A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = (magazine.quarter ?: "EBD"),
                    style = EbdTypography.labelSmall.copy(
                        color = Gold,
                        fontSize = 8.sp
                    )
                )
            }
        } else {
            // Fallback: colored gradient cover with title text
            val colors = coverGradients[
                magazine.coverColorIndex.coerceIn(0, coverGradients.size - 1)
            ]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = (magazine.quarter ?: "EBD").uppercase(),
                    style = EbdTypography.labelSmall.copy(color = Gold)
                )
                Text(
                    text = magazine.title,
                    style = EbdTypography.titleSerif.copy(color = Color(0xFFF0E6D2)),
                    maxLines = 4
                )
                Text("CPAD", style = EbdTypography.labelSmall.copy(color = Gold))
            }
        }
    }
}

/**
 * Larger cover used in the "Continue lendo" card.
 */
@Composable
fun MagazineCoverLarge(
    magazine: Magazine,
    coverLoader: suspend (Magazine) -> java.io.File?,
    onClick: () -> Unit
) {
    val bmp = rememberCoverBitmap(magazine, coverLoader)

    Box(
        modifier = Modifier
            .width(76.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(0.5.dp, Color(0x33F0E6D2), RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        val image = bmp
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = magazine.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val colors = coverGradients[
                magazine.coverColorIndex.coerceIn(0, coverGradients.size - 1)
            ]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = (magazine.quarter ?: "EBD").uppercase(),
                    style = EbdTypography.labelSmall.copy(color = Gold)
                )
                Text(
                    text = magazine.title,
                    style = EbdTypography.titleSerif.copy(color = Color(0xFFF0E6D2)),
                    maxLines = 3
                )
                Text("CPAD", style = EbdTypography.labelSmall.copy(color = Gold))
            }
        }
    }
}


// Keep recently seen images while scrolling, capped by decoded pixel memory.
private val coverBitmaps = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        value.width * value.height * 4
}

@Composable
private fun rememberCoverBitmap(
    magazine: Magazine,
    coverLoader: suspend (Magazine) -> java.io.File?
): ImageBitmap? {
    val key = magazine.coverCacheKey
    var bitmap by remember(key) { mutableStateOf(coverBitmaps.get(key)) }
    LaunchedEffect(key) {
        if (bitmap != null) return@LaunchedEffect
        try {
            val file = coverLoader(magazine) ?: return@LaunchedEffect
            val decoded = withContext(Dispatchers.IO) {
                decodeCoverBitmap(file)?.asImageBitmap()
            }
            if (decoded != null) {
                coverBitmaps.put(key, decoded)
                bitmap = decoded
            } else {
                withContext(Dispatchers.IO) { file.delete() }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("MagazineCover", "Falha ao carregar ou decodificar uma capa", failure)
        }
    }
    return bitmap
}
