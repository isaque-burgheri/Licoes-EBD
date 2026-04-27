package br.com.licoesebd.app.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.licoesebd.app.data.model.AudioAlbum
import br.com.licoesebd.app.data.model.AudioTrack
import br.com.licoesebd.app.data.repository.AudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AudioListUiState {
    data object Loading : AudioListUiState
    data class Loaded(val albums: List<AudioAlbum>) : AudioListUiState
    data class Error(val message: String) : AudioListUiState
}

data class PlayerState(
    val track: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

class AudioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AudioRepository(app)

    private val _state = MutableStateFlow<AudioListUiState>(AudioListUiState.Loading)
    val state: StateFlow<AudioListUiState> = _state.asStateFlow()

    private val _player = MutableStateFlow(PlayerState())
    val player: StateFlow<PlayerState> = _player.asStateFlow()

    private var mp: MediaPlayer? = null
    private var positionPoller: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AudioListUiState.Loading
            try {
                val albums = repo.listAlbums()
                _state.value = AudioListUiState.Loaded(albums)
            } catch (t: Throwable) {
                _state.value = AudioListUiState.Error(t.message ?: "Erro desconhecido")
            }
        }
    }

    /** Play the given track. If it's already current, just resume/pause. */
    fun playTrack(track: AudioTrack) {
        val current = _player.value.track
        if (current?.id == track.id) {
            togglePlayPause()
            return
        }
        // Switch to a new track
        releasePlayer()
        _player.value = PlayerState(track = track, isBuffering = true)
        viewModelScope.launch {
            try {
                val url = repo.streamUrl(track.id)
                val newMp = MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    setOnPreparedListener { player ->
                        _player.value = _player.value.copy(
                            isBuffering = false,
                            isPlaying = true,
                            durationMs = player.duration.coerceAtLeast(0)
                        )
                        player.start()
                        startPositionPoller()
                    }
                    setOnCompletionListener {
                        _player.value = _player.value.copy(
                            isPlaying = false,
                            positionMs = _player.value.durationMs
                        )
                        stopPositionPoller()
                    }
                    setOnErrorListener { _, what, extra ->
                        _player.value = PlayerState(track = track).copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                        true
                    }
                    prepareAsync()
                }
                mp = newMp
            } catch (t: Throwable) {
                _player.value = PlayerState() // reset
            }
        }
    }

    fun togglePlayPause() {
        val player = mp ?: return
        if (player.isPlaying) {
            player.pause()
            _player.value = _player.value.copy(isPlaying = false)
            stopPositionPoller()
        } else {
            player.start()
            _player.value = _player.value.copy(isPlaying = true)
            startPositionPoller()
        }
    }

    fun seekTo(ms: Int) {
        mp?.seekTo(ms)
        _player.value = _player.value.copy(positionMs = ms)
    }

    fun stop() {
        releasePlayer()
        _player.value = PlayerState()
    }

    private fun releasePlayer() {
        stopPositionPoller()
        try { mp?.release() } catch (_: Throwable) {}
        mp = null
    }

    private fun startPositionPoller() {
        positionPoller?.cancel()
        positionPoller = viewModelScope.launch {
            while (true) {
                val p = mp ?: break
                try {
                    val pos = p.currentPosition
                    _player.value = _player.value.copy(positionMs = pos)
                } catch (_: Throwable) { break }
                delay(500)
            }
        }
    }

    private fun stopPositionPoller() {
        positionPoller?.cancel()
        positionPoller = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
