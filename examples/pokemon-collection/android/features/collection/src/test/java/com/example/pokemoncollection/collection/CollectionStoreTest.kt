package com.example.pokemoncollection.collection

import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollectionStoreTest {
    private val catalog get() = Catalog.read(RuntimeEnvironment.getApplication().assets)
    private val pikachu = "sv03.5-025"

    @Test fun emptyStartSearchAndNumericMatching() {
        val store = CollectionStore(catalog, MemoryStorage())
        assertTrue(store.load())
        assertEquals(0, store.state.totalCopies)
        assertEquals(0, store.state.uniqueCards)
        assertTrue(store.state.wishlist.isEmpty())
        assertEquals(listOf(pikachu), catalog.search("  pikACHu ").map { it.id })
        assertEquals(listOf(pikachu), catalog.search("25").map { it.id })
        assertEquals(7, catalog.search("151").size)
        assertEquals(7, catalog.search(" ").size)
        assertTrue(catalog.search("no such card").isEmpty())
    }

    @Test fun committedCopiesAndWishlistSurviveRestartAndDeleteOnlySelectedEntry() {
        val storage = MemoryStorage()
        val store = CollectionStore(catalog, storage)
        store.load()
        assertTrue(store.addCopy(pikachu, 2, "Reverse holo", "Lightly played", "Gift"))
        assertTrue(store.addCopy(pikachu, 3, "Normal", "Near mint", ""))
        assertTrue(store.setWishlist(pikachu, true))
        val firstBytes = storage.bytes
        assertTrue(store.setWishlist(pikachu, true))
        assertEquals(firstBytes, storage.bytes)
        val reopened = CollectionStore(catalog, storage)
        assertTrue(reopened.load())
        assertEquals(5, reopened.state.totalCopies)
        assertEquals(1, reopened.state.uniqueCards)
        assertEquals("Gift", reopened.state.copies.first().note)
        assertEquals("Reverse holo", reopened.state.copies.first().finish)
        assertEquals("Lightly played", reopened.state.copies.first().condition)
        assertTrue(reopened.deleteCopy(reopened.state.copies.first().id))
        assertEquals(3, reopened.state.totalCopies)
        assertTrue(reopened.state.wishlist.containsKey(pikachu))
        assertTrue(reopened.deleteCopy(reopened.state.copies.single().id))
        assertEquals(0, reopened.state.uniqueCards)
        assertTrue(reopened.state.wishlist.containsKey(pikachu))
    }

    @Test fun invalidInputNeverChangesCommittedBytes() {
        val storage = MemoryStorage()
        val store = CollectionStore(catalog, storage)
        store.load()
        store.addCopy(pikachu, 1, "Normal", "Near mint", "kept")
        val before = storage.bytes
        for (quantity in listOf(0, 100)) assertFalse(store.addCopy(pikachu, quantity, "Normal", "Near mint", ""))
        assertFalse(store.addCopy(pikachu, 1, "Holo", "Near mint", ""))
        assertFalse(store.addCopy(pikachu, 1, "Normal", "Perfect", ""))
        assertFalse(store.addCopy(pikachu, 1, "Normal", "Near mint", "x".repeat(501)))
        assertFalse(store.addCopy("unknown", 1, "Normal", "Near mint", ""))
        assertEquals(before, storage.bytes)
        assertEquals(1, store.state.totalCopies)
    }

    @Test fun failedWritePreservesCommitAndRetryCommitsExactlyOnce() {
        val storage = MemoryStorage()
        val store = CollectionStore(catalog, storage)
        store.load()
        store.addCopy(pikachu, 1, "Normal", "Near mint", "committed")
        val before = storage.bytes
        storage.failWrite = true
        assertFalse(store.addCopy(pikachu, 2, "Reverse holo", "Damaged", "retry"))
        assertEquals(before, storage.bytes)
        assertEquals(1, store.state.totalCopies)
        assertNotNull(store.error)
        assertNull(store.notice)
        storage.failWrite = false
        assertTrue(store.retry())
        assertEquals(3, store.state.totalCopies)
        assertNotNull(store.notice)
        assertTrue(store.retry())
        assertEquals(3, store.state.totalCopies)
    }

    @Test fun corruptOrUnsupportedDataIsNeverResetAndCanRetry() {
        for (invalid in listOf("garbage", "{}", "{\"version\":2,\"copies\":[],\"wishlist\":[]}")) {
            val storage = MemoryStorage(invalid)
            val store = CollectionStore(catalog, storage)
            assertFalse(store.load())
            assertFalse(store.loaded)
            assertFalse(store.addCopy(pikachu, 1, "Normal", "Near mint", ""))
            assertEquals(invalid, storage.bytes)
            storage.bytes = null
            assertTrue(store.retry())
            assertTrue(store.loaded)
        }
    }

    @Test fun unreadableFileIsNotMistakenForMissingFile() {
        val storage = MemoryStorage()
        storage.failRead = true
        val store = CollectionStore(catalog, storage)
        assertFalse(store.load())
        assertNotNull(store.error)
        assertFalse(store.setWishlist(pikachu, true))
        storage.failRead = false
        assertTrue(store.retry())
        assertTrue(store.setWishlist(pikachu, true))
    }

    @Test fun actualAtomicFileSurvivesReopenAndRejectedDecodedEntry() {
        val file = File.createTempFile("pokemon", ".json").apply { delete() }
        try {
            val store = CollectionStore(catalog, AtomicCollectionStorage(file))
            store.load()
            assertTrue(store.addCopy(pikachu, 99, "Normal", "Damaged", "Saved"))
            val reopened = CollectionStore(catalog, AtomicCollectionStorage(file))
            assertTrue(reopened.load())
            assertEquals(99, reopened.state.totalCopies)
            val corrupt = file.readText().replace("\"quantity\":99", "\"quantity\":100")
            file.writeText(corrupt)
            assertFalse(CollectionStore(catalog, AtomicCollectionStorage(file)).load())
            assertEquals(corrupt, file.readText())
        } finally { file.delete() }
    }


    @Test fun malformedSavedRecordsAreRejectedWithoutChangingBytes() {
        val good = MemoryStorage()
        val original = CollectionStore(catalog, good)
        original.load()
        original.addCopy(pikachu, 1, "Normal", "Near mint", "Saved")
        original.setWishlist(pikachu, true)
        val valid = requireNotNull(good.bytes)
        val cases = listOf(
            valid + "trailing corruption",
            valid.replace("\"quantity\":1", "\"quantity\":\"1\""),
            valid.replace("\"quantity\":1", "\"quantity\":1.5"),
            valid.replace("Normal", "Holo"),
            valid.replace(pikachu, "unknown"),
            org.json.JSONObject(valid).apply {
                val copies = getJSONArray("copies")
                copies.put(copies.getJSONObject(0))
            }.toString(),
            org.json.JSONObject(valid).apply {
                getJSONArray("wishlist").getJSONObject(0).put("savedAt", -1)
            }.toString(),
        )
        for (invalid in cases) {
            val storage = MemoryStorage(invalid)
            val store = CollectionStore(catalog, storage)
            assertFalse("Must reject damaged saved data", store.load())
            assertFalse(store.setWishlist(pikachu, false))
            assertEquals(invalid, storage.bytes)
        }
    }

    @Test fun invalidUtf8IsNotSilentlyReplacedInsideSavedNotes() {
        val file = File.createTempFile("pokemon-utf8", ".json")
        try {
            val store = CollectionStore(catalog, AtomicCollectionStorage(file))
            file.delete()
            store.load()
            store.addCopy(pikachu, 1, "Normal", "Near mint", "Marker")
            val content = file.readBytes()
            val markerIndex = file.readText().indexOf("Marker")
            content[markerIndex] = 0xFF.toByte()
            file.writeBytes(content)
            assertFalse(CollectionStore(catalog, AtomicCollectionStorage(file)).load())
            assertArrayEquals(content, file.readBytes())
        } finally { file.delete() }
    }


    @Test fun noteLimitCountsUserVisibleCharactersIncludingEmojiAndAccents() {
        for (character in listOf("😺", "👨‍👩‍👧‍👦", "e\u0301")) {
            val storage = MemoryStorage()
            val store = CollectionStore(catalog, storage)
            store.load()
            assertTrue("500 user-visible characters must fit", store.addCopy(pikachu, 1, "Normal", "Near mint", character.repeat(500)))
            val committed = storage.bytes
            assertFalse("501 user-visible characters must be rejected", store.addCopy(pikachu, 1, "Normal", "Near mint", character.repeat(501)))
            assertEquals(committed, storage.bytes)
            val restored = CollectionStore(catalog, storage)
            assertTrue(restored.load())
            assertEquals(character.repeat(500), restored.state.copies.single().note)
        }
    }

    @Test fun savedScopesAndSortingUseActualState() {
        val store = CollectionStore(catalog, MemoryStorage())
        store.load()
        store.addCopy(pikachu, 1, "Normal", "Near mint", "")
        store.addCopy("sv03.5-001", 1, "Normal", "Near mint", "")
        store.setWishlist("sv03.5-199", true)
        assertEquals(listOf("sv03.5-001", pikachu), store.cards(Scope.Collection, "", SortOrder.Name).map { it.id })
        assertEquals(listOf("sv03.5-001", pikachu), store.cards(Scope.Collection, "", SortOrder.Number).map { it.id })
        assertEquals(listOf("sv03.5-199"), store.cards(Scope.Wishlist, "", SortOrder.Recent).map { it.id })
        assertTrue(store.cards(Scope.Collection, "Charizard", SortOrder.Recent).isEmpty())
    }
}

internal class MemoryStorage(var bytes: String? = null) : CollectionStorage {
    var beforeWrite: (() -> Unit)? = null
    var failWrite = false
    var failRead = false
    override fun read(): String? { if (failRead) throw IOException("read failed"); return bytes }
    override fun write(value: String) { beforeWrite?.invoke(); if (failWrite) throw IOException("write failed"); bytes = value }
}
