package br.com.licoesebd.app.data.repository

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Small images first; expensive PDF fallback has its own lane so it cannot
 * occupy the thumbnail slots. A cancelled/failed load never publishes a partial file.
 */
internal class CoverFileCache(
    private val directory: File,
    thumbnailParallelism: Int = 4,
    fallbackParallelism: Int = 1
) {
    private val locks = Array(64) { Mutex() }
    private val thumbnails = Semaphore(thumbnailParallelism)
    private val fallbacks = Semaphore(fallbackParallelism)

    suspend fun get(
        key: String,
        thumbnail: suspend (File) -> Boolean,
        fallback: suspend (File) -> Boolean
    ): File? {
        val name = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            directory.mkdirs()
            val cached = File(directory, "$name.img")
            if (cached.isFile && cached.length() > 0L) return@withLock cached

            val temporary = File.createTempFile(name, ".part", directory)
            try {
                val previewReady = thumbnails.withPermit {
                    attempt(temporary, thumbnail)
                }
                val ready = previewReady || fallbacks.withPermit {
                    attempt(temporary, fallback)
                }
                currentCoroutineContext().ensureActive()
                if (!ready || temporary.length() == 0L) return@withLock null
                if (!temporary.renameTo(cached)) return@withLock null
                cached
            } finally {
                temporary.delete()
            }
        }
    }

    private suspend fun attempt(file: File, load: suspend (File) -> Boolean): Boolean {
        currentCoroutineContext().ensureActive()
        file.writeBytes(byteArrayOf())
        return try {
            load(file).also { currentCoroutineContext().ensureActive() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}
