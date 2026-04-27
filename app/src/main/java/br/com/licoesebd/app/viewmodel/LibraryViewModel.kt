package br.com.licoesebd.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.licoesebd.app.data.model.Magazine
import br.com.licoesebd.app.data.repository.PublicDriveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Loaded(val magazines: List<Magazine>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PublicDriveRepository(app)

    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val _currentPdf = MutableStateFlow<Uri?>(null)
    val currentPdf: StateFlow<Uri?> = _currentPdf.asStateFlow()

    private val _openingTitle = MutableStateFlow<String?>(null)
    val openingTitle: StateFlow<String?> = _openingTitle.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LibraryUiState.Loading
            try {
                val list = repo.listMagazines()
                _state.value = LibraryUiState.Loaded(list)
            } catch (t: Throwable) {
                _state.value = LibraryUiState.Error(t.message ?: "Erro desconhecido")
            }
        }
    }

    fun openMagazine(magazine: Magazine) {
        viewModelScope.launch {
            try {
                _openingTitle.value = magazine.title
                val uri = repo.downloadPdf(magazine)
                _currentPdf.value = uri
            } catch (t: Throwable) {
                _state.value = LibraryUiState.Error("Não foi possível abrir: ${t.message}")
                _openingTitle.value = null
            }
        }
    }

    fun closeReader() {
        _currentPdf.value = null
        _openingTitle.value = null
    }

    /** Used by cover composable: returns a File (or null) for the rendered first page. */
    suspend fun loadCover(magazine: Magazine): java.io.File? = repo.getCoverFile(magazine)
}
