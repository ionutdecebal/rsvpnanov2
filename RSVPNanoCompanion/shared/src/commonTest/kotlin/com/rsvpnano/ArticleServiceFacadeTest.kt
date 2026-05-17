package com.rsvpnano

import com.rsvpnano.app.RsvpSharedFacade
import com.rsvpnano.models.PendingUpload
import com.rsvpnano.persistence.PendingUploadArticleService
import com.rsvpnano.persistence.PendingUploadStore
import com.rsvpnano.persistence.RssFeedStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleServiceFacadeTest {
    @Test
    fun producesBookFileFromPendingUpload() {
        val facade = RsvpSharedFacade(
            pendingUploadStore = object : PendingUploadStore {
                override suspend fun loadAll(): List<PendingUpload> = emptyList()
                override suspend fun saveAll(items: List<PendingUpload>) = Unit
                override suspend fun add(item: PendingUpload) = Unit
                override suspend fun remove(id: String) = Unit
            },
            rssFeedStore = object : RssFeedStore {
                override suspend fun loadAll(): List<String> = emptyList()
                override suspend fun saveAll(items: List<String>) = Unit
            },
            articleService = PendingUploadArticleService(),
        )

        val item = PendingUpload(
            id = "1",
            title = "Shared Article",
            sourceUrl = "https://example.com/story",
            body = "<html><body><p>Hello world.</p></body></html>",
            createdAt = "2026-05-17T10:00:00Z",
        )

        val bookFile = facade.bookFileFor(item)

        assertEquals("Shared Article.rsvp", bookFile.filename)
        assertEquals(true, bookFile.data.decodeToString().contains("Hello world."))
    }
}
