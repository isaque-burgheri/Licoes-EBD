package br.com.licoesebd.app.data.model

/** A whole audio collection for a single magazine (a folder on Drive). */
data class AudioAlbum(
    val id: String,             // Drive folder id
    val title: String,          // e.g. "2020 - 2T"
    val quarter: String?,       // "2T"
    val year: Int?,
    val tracks: List<AudioTrack>,
    val coverColorIndex: Int
)

data class AudioTrack(
    val id: String,             // Drive file id
    val albumId: String,
    val title: String,          // Cleaned (e.g. "Lição 5 — A graça de Deus")
    val rawName: String,
    val orderHint: Int,         // For sorting (0 = album-wide, 1..13 = lessons)
    val mimeType: String,
    val sizeBytes: Long?
)
