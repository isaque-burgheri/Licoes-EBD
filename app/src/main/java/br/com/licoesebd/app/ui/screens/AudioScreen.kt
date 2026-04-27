package br.com.licoesebd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.licoesebd.app.data.model.AudioAlbum
import br.com.licoesebd.app.data.model.AudioTrack
import br.com.licoesebd.app.ui.components.BrandHeader
import br.com.licoesebd.app.ui.theme.*
import br.com.licoesebd.app.viewmodel.AudioListUiState
import br.com.licoesebd.app.viewmodel.PlayerState

private val albumColors = listOf(
    listOf(Color(0xFF16212E), Color(0xFF0A121B)),
    listOf(Color(0xFF4A3818), Color(0xFF2A1F0C)),
    listOf(Color(0xFF2D4A3A), Color(0xFF14241C)),
    listOf(Color(0xFF6B1F24), Color(0xFF3D1014)),
    listOf(Color(0xFF4A2818), Color(0xFF2A160C)),
    listOf(Color(0xFF1F2D4A), Color(0xFF0C1424))
)

@Composable
fun AudioScreen(
    state: AudioListUiState,
    player: PlayerState,
    onTrackClick: (AudioTrack) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onStop: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Bg)
    ) {
        BrandHeader()

        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                is AudioListUiState.Loading -> CenteredText("Sincronizando áudios…")
                is AudioListUiState.Error -> CenteredText("Oops: ${state.message}")
                is AudioListUiState.Loaded -> AlbumsList(
                    albums = state.albums,
                    currentTrackId = player.track?.id,
                    onTrackClick = onTrackClick,
                    showsPlayerBar = player.track != null
                )
            }
        }

        if (player.track != null) {
            PlayerBar(
                player = player,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun AlbumsList(
    albums: List<AudioAlbum>,
    currentTrackId: String?,
    onTrackClick: (AudioTrack) -> Unit,
    showsPlayerBar: Boolean
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎙", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Nenhum áudio ainda",
                    style = EbdTypography.displayMedium.copy(fontSize = 22.sp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Adicione áudios na pasta do Drive seguindo o padrão \u201cEBD 2020-2T-L03.mp3\u201d. Eles aparecerão aqui automaticamente, agrupados por revista.",
                    style = EbdTypography.italicSerif.copy(color = InkSoft, fontSize = 14.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = if (showsPlayerBar) 100.dp else 32.dp
        )
    ) {
        item { Header(albumCount = albums.size) }
        items(albums, key = { it.id }) { album ->
            AlbumBlock(
                album = album,
                currentTrackId = currentTrackId,
                onTrackClick = onTrackClick
            )
        }
    }
}

@Composable
private fun Header(albumCount: Int) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            "PODCAST · EBD",
            style = EbdTypography.label.copy(fontSize = 10.sp, letterSpacing = 2.5.sp)
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text("Ouça as ", style = EbdTypography.displayMedium)
            Text(
                "lições",
                style = EbdTypography.displayMedium.copy(
                    color = Burgundy, fontStyle = FontStyle.Italic
                )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "$albumCount " + (if (albumCount == 1) "trimestre" else "trimestres") + " disponíveis",
            style = EbdTypography.italicSerif.copy(color = InkSoft, fontSize = 14.sp)
        )
    }
}

@Composable
private fun AlbumBlock(
    album: AudioAlbum,
    currentTrackId: String?,
    onTrackClick: (AudioTrack) -> Unit
) {
    val colors = albumColors[album.coverColorIndex.coerceIn(0, albumColors.size - 1)]

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Album header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(colors))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(album.quarter?.replace("T", "º Trimestre"), album.year?.toString())
                        .joinToString(" · ").ifEmpty { "REVISTA" },
                    style = EbdTypography.label.copy(color = Gold, fontSize = 10.sp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = album.title.replace("T", "º Trimestre").ifEmpty { "Lições Bíblicas" },
                    style = EbdTypography.titleSerif.copy(
                        color = Color(0xFFF0E6D2), fontSize = 18.sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${album.tracks.size} faixas",
                    style = EbdTypography.italicSerif.copy(
                        color = Color(0xCCF0E6D2), fontSize = 12.sp
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Gold),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color(0xFF0A121B), fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tracks
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PaperDark)
        ) {
            album.tracks.forEachIndexed { idx, track ->
                TrackRow(
                    track = track,
                    isPlaying = currentTrackId == track.id,
                    onClick = { onTrackClick(track) }
                )
                if (idx < album.tracks.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 56.dp)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color(0x141A1410))
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: AudioTrack,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isPlaying) Burgundy else Color(0x14000000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPlaying) "♪" else "▶",
                color = if (isPlaying) Paper else Ink,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                style = EbdTypography.titleSerif.copy(
                    fontSize = 14.sp,
                    color = if (isPlaying) Burgundy else Ink
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PlayerBar(
    player: PlayerState,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onStop: () -> Unit
) {
    val track = player.track ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1620))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = EbdTypography.titleSerif.copy(
                        color = Color(0xFFF0E6D2), fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Text(
                    formatTime(player.positionMs) + " / " + formatTime(player.durationMs),
                    style = EbdTypography.label.copy(color = Gold, fontSize = 9.sp)
                )
            }
            // Play / pause
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Gold)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (player.isBuffering) {
                    CircularProgressIndicator(
                        color = Color(0xFF0A121B),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        if (player.isPlaying) "❚❚" else "▶",
                        color = Color(0xFF0A121B),
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33F0E6D2))
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color(0xFFF0E6D2), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = if (player.durationMs > 0)
                player.positionMs.toFloat() / player.durationMs.toFloat()
            else 0f,
            onValueChange = { v ->
                if (player.durationMs > 0) onSeek((v * player.durationMs).toInt())
            },
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = Color(0x33F0E6D2)
            )
        )
    }
}

private fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun CenteredText(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Burgundy, strokeWidth = 2.dp)
            Spacer(Modifier.height(16.dp))
            Text(text, style = EbdTypography.italicSerif.copy(color = InkSoft))
        }
    }
}
