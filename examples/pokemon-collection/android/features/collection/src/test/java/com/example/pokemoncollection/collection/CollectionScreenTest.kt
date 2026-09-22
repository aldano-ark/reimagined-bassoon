package com.example.pokemoncollection.collection

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w412dp-h892dp")
class CollectionScreenTest {
    @get:Rule val compose = createComposeRule()
    private val catalog get() = Catalog.read(RuntimeEnvironment.getApplication().assets)
    private val id = "sv03.5-025"
    private fun start(storage: MemoryStorage = MemoryStorage()): CollectionStore {
        val store = CollectionStore(catalog, storage)
        store.load()
        compose.setContent { CollectionTheme { TestScreen(store) } }
        return store
    }
    private fun findPikachu() {
        compose.onNodeWithTag("browse").performClick()
        compose.onNodeWithTag("search").performTextInput(" 25 ")
        compose.onNodeWithTag("card-$id").performClick()
    }
    private fun waitFor(predicate: () -> Boolean) = compose.waitUntil(10_000, predicate)

    @Test fun addSearchWishlistCancelAndConfirmedDeleteFlow() {
        val store = start()
        compose.onNodeWithText("Every collection starts with one card.").assertExists()
        findPikachu()
        compose.onNodeWithText("Add to wishlist").performClick()
        waitFor { store.state.wishlist.containsKey(id) }
        compose.onNodeWithText("Add a copy").performClick()
        compose.onNodeWithTag("quantity").performTextReplacement("2")
        compose.onNodeWithText("Reverse holo").performClick()
        compose.onNodeWithTag("condition").performClick()
        compose.onNodeWithText("Lightly played").performClick()
        compose.onNodeWithTag("note").performScrollTo().performTextInput("Birthday gift")
        compose.onNodeWithTag("save-copy").performClick()
        waitFor { store.state.totalCopies == 2 }
        compose.onNodeWithText("Copy saved").assertExists()
        compose.onNodeWithText("Birthday gift").assertExists()
        compose.onNodeWithText("Add a copy").performClick()
        compose.onNodeWithTag("quantity").performTextReplacement("9")
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(2, store.state.totalCopies) }
        compose.onNodeWithTag("delete-${store.state.copies.single().id}").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(2, store.state.totalCopies) }
        compose.onNodeWithTag("delete-${store.state.copies.single().id}").performScrollTo().performClick()
        compose.onNodeWithText("Delete copies").performClick()
        waitFor { store.state.totalCopies == 0 }
        compose.runOnIdle { assertTrue(store.state.wishlist.containsKey(id)) }
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Every collection starts with one card.").assertExists()
        compose.onNodeWithTag("tab-wishlist").performClick()
        compose.onNodeWithTag("card-$id").assertExists()
        compose.onNodeWithTag("search").performTextInput("missing")
        compose.onNodeWithText("No cards found").assertExists()
    }

    @Test fun failedSaveKeepsFormAndRetryShowsSuccessOnlyAfterCommit() {
        val storage = MemoryStorage()
        val store = start(storage)
        findPikachu()
        compose.onNodeWithText("Add a copy").performClick()
        storage.failWrite = true
        compose.onNodeWithTag("save-copy").performClick()
        waitFor { store.error != null }
        compose.onNodeWithText("Copy saved").assertDoesNotExist()
        compose.onNodeWithTag("quantity").assertExists()
        compose.runOnIdle { assertEquals(0, store.state.totalCopies) }
        storage.failWrite = false
        compose.onNodeWithText("Retry").performClick()
        waitFor { store.state.totalCopies == 1 }
        compose.onNodeWithText("Copy saved").assertExists()
    }

    @Test fun recreationRetainsUnsavedFormAndRelaunchLoadsCommittedState() {
        val storage = MemoryStorage()
        val currentStore = mutableStateOf(CollectionStore(catalog, storage).also { it.load() })
        val restoration = StateRestorationTester(compose)
        restoration.setContent { CollectionTheme { TestScreen(currentStore.value) } }
        findPikachu()
        compose.onNodeWithText("Add a copy").performClick()
        compose.onNodeWithTag("quantity").performTextReplacement("3")
        compose.onNodeWithTag("note").performScrollTo().performTextInput("Keep draft")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("quantity").assertTextContains("3")
        compose.onNodeWithTag("note").assertTextContains("Keep draft")
        compose.onNodeWithTag("save-copy").performClick()
        waitFor { currentStore.value.state.totalCopies == 3 }
        compose.runOnIdle { currentStore.value = CollectionStore(catalog, storage).also { assertTrue(it.load()) } }
        compose.onNodeWithText("Keep draft").assertExists()
        compose.runOnIdle { assertEquals(3, currentStore.value.state.totalCopies) }
    }


    @Test fun saveFinishingDuringRecreationClosesTheDraft() {
        val storage = MemoryStorage()
        val store = CollectionStore(catalog, storage).also { it.load() }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { CollectionTheme { TestScreen(store) } }
        findPikachu()
        compose.onNodeWithText("Add a copy").performClick()
        val started = java.util.concurrent.CountDownLatch(1)
        val resume = java.util.concurrent.CountDownLatch(1)
        storage.beforeWrite = { started.countDown(); resume.await(10, java.util.concurrent.TimeUnit.SECONDS) }
        compose.onNodeWithTag("save-copy").performClick()
        assertTrue(started.await(10, java.util.concurrent.TimeUnit.SECONDS))
        restoration.emulateSavedInstanceStateRestore()
        resume.countDown()
        waitFor { store.state.totalCopies == 1 }
        compose.onNodeWithTag("quantity").assertDoesNotExist()
        compose.onNodeWithText("Copy saved").assertExists()
        compose.runOnIdle { assertEquals(1, store.state.copies.size) }
    }


    @Test fun editingAfterFailedSaveDiscardsPendingRetryAndSavesCurrentDraft() {
        val storage = MemoryStorage()
        val store = start(storage)
        findPikachu()
        compose.onNodeWithText("Add a copy").performClick()
        compose.onNodeWithTag("note").performScrollTo().performTextInput("Original")
        storage.failWrite = true
        compose.onNodeWithTag("save-copy").performClick()
        waitFor { store.error != null }
        compose.onNodeWithText("Retry").assertExists()
        compose.onNodeWithTag("note").performScrollTo().performTextReplacement("Edited after failure")
        compose.onNodeWithText("Retry").assertDoesNotExist()
        storage.failWrite = false
        compose.onNodeWithTag("save-copy").performClick()
        waitFor { store.state.totalCopies == 1 }
        compose.runOnIdle { assertEquals("Edited after failure", store.state.copies.single().note) }
    }

    @Test fun corruptStorageOffersRetryWithoutMutationControls() {
        val storage = MemoryStorage("broken")
        val store = start(storage)
        compose.onNodeWithText("Could not open your collection").assertExists()
        compose.onNodeWithTag("browse").assertDoesNotExist()
        compose.runOnIdle { assertEquals("broken", storage.bytes); storage.bytes = null }
        compose.onNodeWithText("Retry").performClick()
        waitFor { store.loaded }
        compose.onNodeWithTag("browse").assertExists()
    }
}

@Composable private fun TestScreen(store: CollectionStore) {
    var scope by rememberSaveable { mutableStateOf(Scope.Collection) }
    val coroutineScope = rememberCoroutineScope()
    val controller = remember(store) { CollectionController(store, coroutineScope) }
    CollectionScreen(controller, scope) { scope = it }
}
