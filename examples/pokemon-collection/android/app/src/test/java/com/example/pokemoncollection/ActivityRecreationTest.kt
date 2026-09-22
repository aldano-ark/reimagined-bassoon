package com.example.pokemoncollection

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.pokemoncollection.collection.*
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w412dp-h892dp")
class ActivityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun rotationDuringBlockedSaveThenNextMutationPreservesBothCommits() {
        val original = ViewModelProvider(compose.activity)[CollectionModel::class.java]
        compose.waitUntil(10_000) { original.store.loaded && !original.controller.busy }
        val started = CountDownLatch(1)
        val resume = CountDownLatch(1)
        compose.runOnIdle {
            original.controller.perform {
                started.countDown()
                check(resume.await(10, TimeUnit.SECONDS))
                original.store.addCopy("sv03.5-025", 2, "Reverse holo", "Lightly played", "Rotation")
            }
        }
        assertTrue(started.await(10, TimeUnit.SECONDS))
        compose.activityRule.scenario.recreate()
        val recreated = ViewModelProvider(compose.activity)[CollectionModel::class.java]
        assertSame(original, recreated)
        assertTrue(recreated.controller.busy)
        resume.countDown()
        compose.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            !recreated.controller.busy
        }
        compose.runOnIdle {
            assertEquals(2, recreated.store.state.totalCopies)
            recreated.controller.perform { recreated.store.setWishlist("sv03.5-199", true) }
        }
        compose.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            !recreated.controller.busy
        }
        val restored = CollectionStore(Catalog.read(compose.activity.assets), AtomicCollectionStorage(File(compose.activity.filesDir, "pokemon-collection-v1.json")))
        assertTrue(restored.load())
        assertEquals(2, restored.state.totalCopies)
        assertEquals("Rotation", restored.state.copies.single().note)
        assertTrue(restored.state.wishlist.containsKey("sv03.5-199"))
    }
}
