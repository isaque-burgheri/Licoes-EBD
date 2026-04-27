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
 * Loads the first PDF page lazily; falls back to the colored gradient cover.
 */
@Composable
fun MagazineCover(
    magazine: Magazine,
    coverLoader: suspend (Magazine) -> java.io.File?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var bmp by remember(magazine.id) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(magazine.id) {
        val f = coverLoader(magazine) ?: return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) {
            try {
                android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
            } catch (_: Throwable) { null }
        }
        bmp = decoded
    }

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
    var bmp by remember(magazine.id) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(magazine.id) {
        val f = coverLoader(magazine) ?: return@LaunchedEffect
        val decoded = withContext(Dispatchers.IO) {
            try {
                android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
            } catch (_: Throwable) { null }
        }
        bmp = decoded
    }

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
