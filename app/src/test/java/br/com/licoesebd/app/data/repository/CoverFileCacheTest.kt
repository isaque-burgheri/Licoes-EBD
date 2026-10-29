package br.com.licoesebd.app.data.repository

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoverFileCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun directory() = temporary.newFolder()

    @Test fun thumbnailAvoidsDownloadingPdf() = runBlocking {
        var pdfDownloads = 0
        val cache = CoverFileCache(directory())
        val result = cache.get("revista:1", { it.writeText("preview"); true }, {
            pdfDownloads++
            it.writeText("pdf cover")
            true
        })
        assertEquals("preview", result?.readText())
        assertEquals(0, pdfDownloads)
    }

    @Test fun diskCacheSurvivesNewLoaderAndFileChangesInvalidateIt() = runBlocking {
        val dir = directory()
        var downloads = 0
        val loader: suspend (File) -> Boolean = { downloads++; it.writeText("image"); true }
        CoverFileCache(dir).get("revista:1", loader, { error("PDF not expected") })
        CoverFileCache(dir).get("revista:1", loader, { error("PDF not expected") })
        CoverFileCache(dir).get("revista:2", loader, { error("PDF not expected") })
        assertEquals(2, downloads)
    }

    @Test fun simultaneousRequestsForSameCoverDownloadOnce() = runBlocking {
        var downloads = 0
        val cache = CoverFileCache(directory())
        val results = List(6) {
            async {
                cache.get("same-cover", {
                    downloads++
                    delay(20)
                    it.writeText("image")
                    true
                }, { error("PDF not expected") })
            }
        }.awaitAll()
        assertEquals(1, downloads)
        assertTrue(results.all { it != null && it == results.first() })
    }

    @Test fun missingThumbnailFallsBackToPdf() = runBlocking {
        var pdfDownloads = 0
        val result = CoverFileCache(directory()).get("missing", { throw IOException("expired") }, {
            pdfDownloads++
            it.writeText("rendered")
            true
        })
        assertEquals("rendered", result?.readText())
        assertEquals(1, pdfDownloads)
    }

    @Test fun partialFailuresAreNotCachedAndCanBeRetried() = runBlocking {
        val dir = directory()
        val cache = CoverFileCache(dir)
        assertNull(cache.get("failed", { it.writeText("partial"); false }, { false }))
        assertTrue(dir.listFiles().orEmpty().isEmpty())
        val result = cache.get("failed", { it.writeText("complete"); true }, { false })
        assertEquals("complete", result?.readText())
    }

    @Test fun cancellationDeletesPartialFileWithoutStartingPdfFallback() = runBlocking {
        val dir = directory()
        val cache = CoverFileCache(dir)
        val started = CompletableDeferred<Unit>()
        var pdfDownloads = 0
        val job = launch {
            cache.get("cancelled", {
                it.writeText("partial")
                started.complete(Unit)
                delay(Long.MAX_VALUE)
                true
            }, { pdfDownloads++; true })
        }
        started.await()
        job.cancelAndJoin()
        assertEquals(0, pdfDownloads)
        assertTrue(dir.listFiles().orEmpty().isEmpty())
        assertNotNull(cache.get("cancelled", { it.writeText("retry"); true }, { false }))
    }

    @Test fun slowPdfFallbackDoesNotBlockOtherThumbnails() = runBlocking {
        val cache = CoverFileCache(directory(), thumbnailParallelism = 1)
        val fallbackStarted = CompletableDeferred<Unit>()
        val releaseFallback = CompletableDeferred<Unit>()
        val slow = async {
            cache.get("slow", { false }, {
                fallbackStarted.complete(Unit)
                releaseFallback.await()
                it.writeText("pdf")
                true
            })
        }
        fallbackStarted.await()
        try {
            val quick = withTimeout(2000) {
                cache.get("quick", { it.writeText("thumbnail"); true }, { false })
            }
            assertEquals("thumbnail", quick?.readText())
        } finally {
            releaseFallback.complete(Unit)
        }
        assertNotNull(slow.await())
    }

    @Test fun thumbnailConcurrencyIsBounded() = runBlocking {
        val cache = CoverFileCache(directory(), thumbnailParallelism = 2)
        var active = 0
        var maximum = 0
        List(10) { index ->
            async {
                cache.get("cover-$index", {
                    active++
                    maximum = maxOf(maximum, active)
                    delay(10)
                    it.writeText("image")
                    active--
                    true
                }, { false })
            }
        }.awaitAll()
        assertEquals(2, maximum)
    }
}
