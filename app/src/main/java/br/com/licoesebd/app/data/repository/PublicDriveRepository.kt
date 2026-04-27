package br.com.licoesebd.app.data.repository

import android.content.Context
import android.net.Uri
import br.com.licoesebd.app.BuildConfig
import br.com.licoesebd.app.data.model.Magazine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Reads a publicly-shared Google Drive folder via the Drive REST API
 * using an API key — no OAuth, no Google Sign-In, no SHA-1 needed.
 *
 * The folder must be shared as "Anyone with the link can view".
 */
class PublicDriveRepository(private val context: Context) {

    /**
     * Folder ID extracted from:
     * https://drive.google.com/drive/folders/1E_ANsYGpvFPOjNV8VguC7EdfTodDljLm
     */
    private val rootFolderId = "1E_ANsYGpvFPOjNV8VguC7EdfTodDljLm"

    private val apiKey: String get() = BuildConfig.DRIVE_API_KEY

    suspend fun listMagazines(): List<Magazine> = withContext(Dispatchers.IO) {
        val all = mutableListOf<DriveFileLite>()
        collectPdfsRecursive(rootFolderId, all)
        all.mapIndexed { index, f -> f.toMagazine(index) }
            .sortedWith(
                compareByDescending<Magazine> { it.year ?: 0 }
                    .thenByDescending { it.quarter ?: "" }
                    .thenBy { it.title }
            )
    }

    private fun collectPdfsRecursive(folderId: String, sink: MutableList<DriveFileLite>) {
        var pageToken: String? = null
        do {
            val q = URLEncoder.encode("'$folderId' in parents and trashed = false", "UTF-8")
            val fields = URLEncoder.encode(
                "nextPageToken, files(id,name,mimeType,size,modifiedTime,thumbnailLink,webViewLink)",
                "UTF-8"
            )
            val pageParam = pageToken?.let { "&pageToken=${URLEncoder.encode(it, "UTF-8")}" } ?: ""
            val url = "https://www.googleapis.com/drive/v3/files" +
                "?q=$q&fields=$fields&pageSize=200&key=$apiKey$pageParam"

            val json = httpGetJson(url)
            val files = json.optJSONArray("files") ?: break
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val mime = f.optString("mimeType")
                val item = DriveFileLite(
                    id = f.getString("id"),
                    name = f.getString("name"),
                    mimeType = mime,
                    sizeBytes = f.optString("size").toLongOrNull(),
                    modifiedTimeMillis = parseRfc3339(f.optString("modifiedTime")),
                    thumbnailLink = f.optString("thumbnailLink").ifEmpty { null },
                    webViewLink = f.optString("webViewLink").ifEmpty { null }
                )
                when (mime) {
                    "application/vnd.google-apps.folder" -> collectPdfsRecursive(item.id, sink)
                    "application/pdf" -> sink.add(item)
                }
            }
            pageToken = json.optString("nextPageToken").ifEmpty { null }
        } while (pageToken != null)
    }

    /** Downloads a PDF to local cache and returns a file:// Uri usable by the PDF viewer. */
    suspend fun downloadPdf(magazine: Magazine): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "magazines").apply { mkdirs() }
        val out = File(dir, "${magazine.id}.pdf")
        if (!out.exists() || out.length() == 0L) {
            downloadPublicFile(magazine.id, out)
        }
        Uri.fromFile(out)
    }

    /**
     * Downloads a publicly-shared Drive file to the given target.
     * Uses the public "uc?export=download" endpoint, which works for any file
     * shared as "Anyone with the link can view" — no API key required, no auth.
     *
     * For files larger than ~25MB, Drive serves an HTML interstitial with a
     * confirmation token. We detect this and re-request with the token.
     */
    private fun downloadPublicFile(fileId: String, target: File) {
        val cookies = mutableMapOf<String, String>()
        val firstUrl = "https://drive.google.com/uc?export=download&id=$fileId"

        // First attempt — may return either the binary, a 302 to the binary,
        // or an HTML interstitial for big files.
        var resp = openWithCookies(firstUrl, cookies)
        try {
            val contentType = resp.contentType?.lowercase().orEmpty()
            val isHtml = contentType.contains("text/html")
            if (isHtml) {
                // Read the interstitial, find the confirm token, retry.
                val body = resp.inputStream.bufferedReader().readText()
                resp.disconnect()
                val token = Regex("confirm=([0-9A-Za-z_-]+)").find(body)
                    ?.groupValues?.get(1)
                    ?: extractCookieToken(cookies)
                    ?: throw RuntimeException("Drive: token de confirmação não encontrado (arquivo possivelmente privado)")
                val confirmUrl = "https://drive.google.com/uc?export=download&confirm=$token&id=$fileId"
                resp = openWithCookies(confirmUrl, cookies)
            }
            if (resp.code !in 200..299) {
                throw RuntimeException("HTTP ${resp.code} ao baixar")
            }
            FileOutputStream(target).use { output ->
                resp.inputStream.copyTo(output, bufferSize = 32 * 1024)
            }
        } finally {
            resp.disconnect()
        }
    }

    private data class HttpResp(
        val code: Int,
        val contentType: String?,
        val inputStream: java.io.InputStream,
        private val conn: HttpURLConnection
    ) {
        fun disconnect() { try { inputStream.close() } catch (_: Throwable) {} ; conn.disconnect() }
    }

    private fun openWithCookies(url: String, cookieJar: MutableMap<String, String>): HttpResp {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 LicoesEBD/1.0")
            if (cookieJar.isNotEmpty()) {
                val cookieHeader = cookieJar.entries.joinToString("; ") { "${it.key}=${it.value}" }
                setRequestProperty("Cookie", cookieHeader)
            }
        }
        // Capture Set-Cookie headers
        val setCookies = conn.headerFields["Set-Cookie"] ?: emptyList()
        for (c in setCookies) {
            val pair = c.substringBefore(";")
            val eq = pair.indexOf('=')
            if (eq > 0) cookieJar[pair.substring(0, eq).trim()] = pair.substring(eq + 1).trim()
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            ?: throw RuntimeException("Sem corpo na resposta (HTTP $code)")
        return HttpResp(code, conn.contentType, stream, conn)
    }

    private fun extractCookieToken(cookies: Map<String, String>): String? {
        // Big files set a cookie like "download_warning_..." with the confirm token as value
        return cookies.entries.firstOrNull { it.key.startsWith("download_warning") }?.value
    }

    /**
     * Returns a cached cover bitmap path for the magazine.
     * Downloads the PDF if needed, then renders the first page as PNG.
     * Returns null if anything fails (UI will fall back to the colored cover).
     */
    suspend fun getCoverFile(magazine: Magazine): File? = withContext(Dispatchers.IO) {
        val coversDir = File(context.cacheDir, "covers").apply { mkdirs() }
        val coverFile = File(coversDir, "${magazine.id}.png")
        if (coverFile.exists() && coverFile.length() > 0) return@withContext coverFile
        try {
            val pdfUri = downloadPdf(magazine)
            val pdfFile = File(pdfUri.path ?: return@withContext null)
            val pfd = android.os.ParcelFileDescriptor.open(
                pdfFile,
                android.os.ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            try {
                if (renderer.pageCount == 0) return@withContext null
                val page = renderer.openPage(0)
                val targetW = 480
                val ratio = page.height.toFloat() / page.width.toFloat()
                val bmp = android.graphics.Bitmap.createBitmap(
                    targetW,
                    (targetW * ratio).toInt().coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null,
                    android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                FileOutputStream(coverFile).use { out ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                }
                bmp.recycle()
            } finally {
                renderer.close()
                pfd.close()
            }
            coverFile
        } catch (_: Throwable) {
            null
        }
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
            if (code !in 200..299) {
                throw RuntimeException("Drive API HTTP $code: $body")
            }
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }
}

private data class DriveFileLite(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val modifiedTimeMillis: Long?,
    val thumbnailLink: String?,
    val webViewLink: String?
)

private fun DriveFileLite.toMagazine(index: Int): Magazine {
    val (q, y, cleanTitle) = parseQuarterYear(name)
    return Magazine(
        id = id,
        title = cleanTitle,
        rawName = name,
        quarter = q,
        year = y,
        sizeBytes = sizeBytes,
        modifiedTimeMillis = modifiedTimeMillis,
        mimeType = mimeType,
        thumbnailLink = thumbnailLink,
        webViewLink = webViewLink,
        coverColorIndex = index % 6
    )
}

private fun parseRfc3339(s: String): Long? {
    if (s.isBlank()) return null
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        fmt.parse(s)?.time
    } catch (_: Throwable) { null }
}

/**
 * Heuristics to pull "1934 - 1° Trimestre", "1942 - 2° Semestre", "1T 2026" etc out of the filename.
 */
private fun parseQuarterYear(rawName: String): Triple<String?, Int?, String> {
    val baseName = rawName.removeSuffix(".pdf").removeSuffix(".PDF")
    // Accept years from 1900..2099 — the collection has revistas from 1930s onward.
    val yearRegex = Regex("(19\\d{2}|20\\d{2})")
    // Match: "1° Trimestre", "1º Trim", "1 trimestre", "1T", "2° Semestre", "2 sem"
    val periodRegex = Regex(
        "(?i)(\\d)\\s*[º°o]?\\s*(trimestre|trim|t|semestre|sem|s)\\b"
    )

    val year = yearRegex.find(baseName)?.value?.toIntOrNull()
    val match = periodRegex.find(baseName)
    val periodNum = match?.groupValues?.get(1)?.toIntOrNull()
    val periodKind = match?.groupValues?.get(2)?.lowercase()
    val periodLabel = when {
        periodNum == null -> null
        periodKind?.startsWith("s") == true -> "${periodNum}S"
        else -> "${periodNum}T"
    }
    val combined = when {
        periodLabel != null && year != null -> "$periodLabel $year"
        periodLabel != null -> periodLabel
        else -> null
    }

    var clean = baseName
    // Remove the prefix "Lições Bíblicas" used in this collection
    clean = clean.replace(Regex("(?i)li(ç|c)(ões|oes)?\\s+b(í|i)blicas"), "")
    if (year != null) clean = clean.replace(year.toString(), "")
    match?.let { clean = clean.replace(it.value, "") }
    clean = clean.replace(Regex("[_\\-]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '-', '·', '|', ',')

    if (clean.isEmpty()) clean = if (periodLabel != null) "Lições Bíblicas" else baseName
    return Triple(combined, year, clean)
}
