package br.com.licoesebd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.licoesebd.app.ui.theme.*

@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, GoldDeep, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    color = GoldDeep,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Row {
                Text("Lições ", style = EbdTypography.titleSerif)
                Text(
                    "EBD",
                    style = EbdTypography.titleSerif.copy(
                        color = Burgundy,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}

@Composable
fun VerseCard(
    verse: String,
    reference: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Night, Color(0xFF0D1620))))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "VERSÍCULO DO DIA",
                style = EbdTypography.label.copy(color = Gold)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "\u201C$verse\u201D",
                style = EbdTypography.italicSerif.copy(
                    color = Color(0xFFF0E6D2),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                reference.uppercase(),
                style = EbdTypography.label.copy(color = Gold)
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title.uppercase(),
            style = EbdTypography.titleSerif.copy(
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
        )
        if (action != null) {
            Text(
                action.uppercase(),
                style = EbdTypography.labelSmall.copy(color = Burgundy),
                modifier = Modifier
            )
        }
    }
}
