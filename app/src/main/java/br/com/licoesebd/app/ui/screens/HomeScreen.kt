package br.com.licoesebd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.licoesebd.app.data.model.Magazine
import br.com.licoesebd.app.ui.components.*
import br.com.licoesebd.app.ui.theme.*
import br.com.licoesebd.app.viewmodel.LibraryUiState
import java.io.File

@Composable
fun HomeScreen(
    state: LibraryUiState,
    userName: String?,
    coverLoader: suspend (Magazine) -> File?,
    onMagazineClick: (Magazine) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Bg)
    ) {
        BrandHeader()

        when (state) {
            is LibraryUiState.Loading -> LoadingState()
            is LibraryUiState.Error -> ErrorState(state.message, onRefresh)
            is LibraryUiState.Loaded -> LoadedHome(
                magazines = state.magazines,
                userName = userName,
                coverLoader = coverLoader,
                onMagazineClick = onMagazineClick
            )
        }
    }
}

@Composable
private fun LoadedHome(
    magazines: List<Magazine>,
    userName: String?,
    coverLoader: suspend (Magazine) -> File?,
    onMagazineClick: (Magazine) -> Unit
) {
    // Group by year (newest first); items with no year go to "Sem data"
    val groups: List<Pair<String, List<Magazine>>> = remember(magazines) {
        magazines
            .groupBy { it.year ?: 0 }
            .toSortedMap(compareByDescending { it })
            .map { (year, list) ->
                val label = if (year == 0) "Sem data" else year.toString()
                label to list.sortedBy { it.quarter ?: "" }
            }
    }

    val mostRecent = magazines.firstOrNull()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Greeting(userName, totalCount = magazines.size) }

        item {
            VerseCard(
                verse = "Lâmpada para os meus pés é tua palavra, e luz para o meu caminho.",
                reference = "Salmos 119:105"
            )
        }

        if (mostRecent != null) {
            item { SectionTitle("Mais recente") }
            item {
                CurrentMagazineCard(
                    magazine = mostRecent,
                    coverLoader = coverLoader,
                    onClick = { onMagazineClick(mostRecent) }
                )
            }
        }

        item {
            SectionTitle(
                title = "Sua biblioteca",
                action = "${magazines.size} revistas"
            )
        }

        groups.forEach { (yearLabel, list) ->
            item(key = "y_$yearLabel") {
                YearLabel(year = yearLabel, count = list.size)
            }
            // Build rows of 3 covers
            val rows = list.chunked(3)
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { mag ->
                        MagazineCover(
                            magazine = mag,
                            coverLoader = coverLoader,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(3f / 4.2f),
                            onClick = { onMagazineClick(mag) }
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(3f / 4.2f)
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun Greeting(userName: String?, totalCount: Int) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            "EBD · ASSEMBLEIA DE DEUS",
            style = EbdTypography.label.copy(fontSize = 10.sp, letterSpacing = 2.5.sp)
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text("Bom dia, ", style = EbdTypography.displayMedium)
            Text(
                userName?.let { "${it.split(" ").first()}!" } ?: "irmão!",
                style = EbdTypography.displayMedium.copy(
                    color = Burgundy,
                    fontStyle = FontStyle.Italic
                )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "$totalCount revistas no seu acervo",
            style = EbdTypography.italicSerif.copy(
                color = InkSoft,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun YearLabel(year: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            year,
            style = EbdTypography.titleSerif.copy(
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (count == 1) "1 revista" else "$count revistas",
            style = EbdTypography.italicSerif.copy(
                color = InkSoft, fontSize = 13.sp
            )
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0x261A1410))
        )
    }
}

@Composable
private fun CurrentMagazineCard(
    magazine: Magazine,
    coverLoader: suspend (Magazine) -> File?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PaperDark)
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MagazineCoverLarge(
            magazine = magazine,
            coverLoader = coverLoader,
            onClick = onClick
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x146B1F24))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "MAIS RECENTE",
                    style = EbdTypography.label.copy(color = Burgundy, fontSize = 9.sp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                magazine.title,
                style = EbdTypography.titleSerif.copy(fontSize = 16.sp, lineHeight = 19.sp),
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(magazine.quarter, magazine.year?.toString())
                    .joinToString(" · ").ifEmpty { "Revista EBD" },
                style = EbdTypography.italicSerif.copy(fontSize = 12.sp, color = InkSoft)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Burgundy, strokeWidth = 2.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Sincronizando com o Drive…",
                style = EbdTypography.italicSerif.copy(color = InkSoft)
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oops…", style = EbdTypography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = EbdTypography.body,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Ink)
                    .clickable { onRetry() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Tentar novamente",
                    style = EbdTypography.titleSerif.copy(color = Paper, fontSize = 13.sp))
            }
        }
    }
}
