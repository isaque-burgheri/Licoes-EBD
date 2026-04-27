package br.com.licoesebd.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.licoesebd.app.ui.screens.AudioScreen
import br.com.licoesebd.app.ui.screens.HomeScreen
import br.com.licoesebd.app.ui.theme.*
import br.com.licoesebd.app.viewmodel.AudioViewModel
import br.com.licoesebd.app.viewmodel.LibraryViewModel
import java.io.File

private enum class Tab { Revistas, Audios }

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val libVm: LibraryViewModel = viewModel()
    val audioVm: AudioViewModel = viewModel()

    var tab by remember { mutableStateOf(Tab.Revistas) }

    val libState by libVm.state.collectAsState()
    val pdfUri by libVm.currentPdf.collectAsState()
    val audioState by audioVm.state.collectAsState()
    val playerState by audioVm.player.collectAsState()

    // Open PDF externally when ViewModel sets a uri.
    LaunchedEffect(pdfUri) {
        val uri = pdfUri ?: return@LaunchedEffect
        try {
            val file = File(uri.path ?: error("URI sem path"))
            val contentUri = FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                ctx.startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                val chooser = Intent.createChooser(intent, "Abrir revista com…").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(chooser)
            }
        } catch (t: Throwable) {
            Toast.makeText(ctx, "Não foi possível abrir o PDF: ${t.message}",
                Toast.LENGTH_LONG).show()
        } finally {
            libVm.closeReader()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Bg)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                Tab.Revistas -> HomeScreen(
                    state = libState,
                    userName = null,
                    coverLoader = { mag -> libVm.loadCover(mag) },
                    onMagazineClick = { libVm.openMagazine(it) },
                    onRefresh = { libVm.refresh() }
                )
                Tab.Audios -> AudioScreen(
                    state = audioState,
                    player = playerState,
                    onTrackClick = { audioVm.playTrack(it) },
                    onTogglePlay = { audioVm.togglePlayPause() },
                    onSeek = { audioVm.seekTo(it) },
                    onStop = { audioVm.stop() }
                )
            }
        }

        BottomTabs(
            current = tab,
            onSelect = { tab = it }
        )
    }
}

@Composable
private fun BottomTabs(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF2FAF6EC))
            .padding(top = 6.dp, bottom = 18.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabButton(
            label = "Revistas",
            icon = "📖",
            active = current == Tab.Revistas,
            onClick = { onSelect(Tab.Revistas) }
        )
        TabButton(
            label = "Áudios",
            icon = "🎙",
            active = current == Tab.Audios,
            onClick = { onSelect(Tab.Audios) }
        )
    }
}

@Composable
private fun TabButton(
    label: String,
    icon: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = EbdTypography.label.copy(
                fontSize = 9.sp,
                color = if (active) Burgundy else InkSoft
            )
        )
    }
}
