package br.com.licoesebd.app.data.repository

import android.content.Context
import br.com.licoesebd.app.BuildConfig
import br.com.licoesebd.app.data.model.AudioAlbum
import br.com.licoesebd.app.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Reads audio podcasts from a single, flat, publicly-shared Drive folder.
 *
 * Expected naming pattern (case-insensitive):
 *   EBD 2020-2T-L03.mp3      → ano 2020, 2º trimestre, lição 3
 *   EBD 2020-2T-Revista.mp3  → ano 2020, 2º trimestre, áudio da revista inteira
 *   ebd 2020-2t-l3.m4a       → mesma coisa (case insensitive, sem zero à esquerda)
 *
 * Files are grouped into AudioAlbums by (year, quarter). Each album becomes one
 * "podcast" of the corresponding magazine; tracks inside it are sorted with the
 * "Revista completa" (lição 0) first, then Lição 1, 2, 3...
 *
 * Files that don't match the pattern are still included in a "Outros áudios"
 * album so nothing is lost.
 */
class AudioRepository(private val context: Context) {

    private val rootFolderId: String get() = BuildConfig.AUDIO_FOLDER_ID
    private val apiKey: String get() = BuildConfig.DRIVE_API_KEY

    suspend fun listAlbums(): List<AudioAlbum> = withContext(Dispatchers.IO) {
        if (rootFolderId.isBlank()) return@withContext emptyList()

        val files = listAudioFilesRecursive(rootFolderId)
        if (files.isEmpty()) return@withContext emptyList()

        // Parse each file and group by (year, quarter)
        data class Parsed(
            val file: Lite,
            val year: Int?,
            val quarter: String?,   // "1T".."4T" or null
            val orderHint: Int,     // 0 = revista completa, 1..N = lições
            val trackTitle: String
        )

        val parsed = files.map { f ->
            val info = parseAudioName(f.name)
            Parsed(
                file = f,
                year = info.year,
                quarter = info.quarter,
                orderHint = info.orderHint,
                trackTitle = info.trackTitle
            )
        }

        val groups = parsed.groupBy { it.year to it.quarter }

        groups.entries
            .mapIndexed { index, (key, list) ->
                val (year, quarter) = key
                val albumTitle = when {
                    year != null && quarter != null -> "$quarter · $year"
                    year != null -> "$year"
                    else -> "Outros áudios"
                }
                val tracks = list.sortedBy { it.orderHint }.map { p ->
                    AudioTrack(
                        id = p.file.id,
                        albumId = "${year ?: 0}_${quarter ?: "x"}",
                        title = p.trackTitle,
                        rawName = p.file.name,
                        orderHint = p.orderHint,
                        mimeType = p.file.mimeType,
                        sizeBytes = p.file.sizeBytes
                    )
                }
                AudioAlbum(
                    id = "${year ?: 0}_${quarter ?: "x"}",
                    title = albumTitle,
                    quarter = quarter,
                    year = year,
                    tracks = tracks,
                    coverColorIndex = index % 6
                )
            }
            .sortedWith(
                compareByDescending<AudioAlbum> { it.year ?: 0 }
                    .thenByDescending { it.quarter ?: "" }
            )
    }

    /** Streamable URL for a Drive audio file. MediaPlayer follows the redirect fine. */
    fun streamUrl(trackId: String): String =
        "https://drive.google.com/uc?export=download&id=$trackId"

    // ---- internals ----

    private data class Lite(
        val id: String,
        val name: String,
        val mimeType: String,
        val sizeBytes: Long?
    )

    private fun listAudioFilesRecursive(folderId: String): List<Lite> {
        val out = mutableListOf<Lite>()
        var pageToken: String? = null
        do {
            val q = URLEncoder.encode(
                "'$folderId' in parents and trashed = false", "UTF-8"
            )
            val fields = URLEncoder.encode(
                "nextPageToken, files(id,name,mimeType,size)", "UTF-8"
            )
            val pageParam = pageToken?.let { "&pageToken=${URLEncoder.encode(it, "UTF-8")}" } ?: ""
            val url = "https://www.googleapis.com/drive/v3/files" +
                "?q=$q&fields=$fields&pageSize=200&key=$apiKey$pageParam"

            val json = httpGetJson(url)
            val files = json.optJSONArray("files") ?: break
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val mime = f.optString("mimeType")
                val item = Lite(
                    id = f.getString("id"),
                    name = f.getString("name"),
                    mimeType = mime,
                    sizeBytes = f.optString("size").toLongOrNull()
                )
                when {
                    mime == "application/vnd.google-apps.folder" ->
                        out.addAll(listAudioFilesRecursive(item.id))
                    mime.startsWith("audio/") -> out.add(item)
                }
            }
            pageToken = json.optString("nextPageToken").ifEmpty { null }
        } while (pageToken != null)
        return out
    }

    private fun httpGetJson(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().readText()
                else conn.errorStream?.bufferedReader()?.readText() ?: ""
            if (code !in 200..299) throw RuntimeException("Drive API HTTP $code: $body")
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }
}

// -- file-name parsing -------------------------------------------------------

private data class AudioInfo(
    val year: Int?,
    val quarter: String?,    // "1T", "2T", ...
    val orderHint: Int,      // 0 = revista, N = lição N, 999 = unknown
    val trackTitle: String   // human-friendly, e.g. "Lição 3" or "Revista completa"
)

/**
 * Parses names like:
 *   "EBD 2020-2T-L03.mp3", "ebd 2020-2t-l3", "EBD 2020-2T-Revista.mp3"
 * Tolerates extra prefixes/suffixes; falls back gracefully when parts are missing.
 */
private fun parseAudioName(rawName: String): AudioInfo {
    val base = rawName.substringBeforeLast('.')

    val year = Regex("(19\\d{2}|20\\d{2})").find(base)?.value?.toIntOrNull()
    val quarter = Regex("(?i)(\\d)\\s*[º°o]?\\s*T\\b")
        .find(base)?.groupValues?.get(1)
        ?.let { "${it}T" }

    // Lesson number: "L03", "L3", "Lição 3", "Licao 03", or "00" leading
    val lessonMatch = Regex("(?i)L\\s*0*(\\d{1,2})").find(base)
        ?: Regex("(?i)li[çc][aã]o\\s*0*(\\d{1,2})").find(base)
    val lessonNum = lessonMatch?.groupValues?.get(1)?.toIntOrNull()

    // Detect "revista completa" / "geral" / "introdução"
    val isWholeRevista = Regex("(?i)\\b(revista|geral|introdu[çc][aã]o|completa|vis[aã]o)\\b")
        .containsMatchIn(base)

    val orderHint = when {
        isWholeRevista -> 0
        lessonNum != null -> lessonNum
        else -> 999
    }

    val trackTitle = when {
        isWholeRevista -> "Revista completa"
        lessonNum != null -> "Lição ${lessonNum.toString().padStart(2, '0')}"
        else -> base // last-resort fallback
    }

    return AudioInfo(year, quarter, orderHint, trackTitle)
}
