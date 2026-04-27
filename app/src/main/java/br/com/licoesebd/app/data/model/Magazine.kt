package br.com.licoesebd.app.data.model

data class Magazine(
    val id: String,           // Drive file id
    val title: String,        // Cleaned title
    val rawName: String,      // Original file name
    val quarter: String?,     // e.g. "1T 2026"
    val year: Int?,           // e.g. 2026
    val sizeBytes: Long?,
    val modifiedTimeMillis: Long?,
    val mimeType: String,
    val thumbnailLink: String?,
    val webViewLink: String?,
    val coverColorIndex: Int  // 0..5 to pick a gradient
) {
    val isPdf: Boolean get() = mimeType == "application/pdf"
}

data class ReadingProgress(
    val magazineId: String,
    val pageIndex: Int,
    val totalPages: Int,
    val updatedAt: Long
) {
    val percent: Float get() = if (totalPages > 0) pageIndex.toFloat() / totalPages else 0f
}
