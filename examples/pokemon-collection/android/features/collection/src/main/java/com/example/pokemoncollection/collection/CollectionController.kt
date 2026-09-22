package com.example.pokemoncollection.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The app gives this controller a retained ViewModel scope so writes survive configuration changes. */
public class CollectionController(public val store: CollectionStore, private val scope: CoroutineScope) {
    public var busy: Boolean by mutableStateOf(false)
        private set

    public fun perform(action: () -> Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            try { withContext(Dispatchers.IO) { action() } }
            finally { busy = false }
        }
    }
}
