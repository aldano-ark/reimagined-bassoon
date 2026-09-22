package com.example.pokemoncollection.collection

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Empty collection", widthDp = 412, heightDp = 892, showBackground = true)
@Composable private fun EmptyPreview() { PreviewCollection(false) }

@Preview(name = "Owned collection", widthDp = 412, heightDp = 892, showBackground = true)
@Composable private fun OwnedPreview() { PreviewCollection(true) }

@Preview(name = "Dark wishlist", widthDp = 412, heightDp = 892, showBackground = true)
@Composable private fun DarkWishlistPreview() { PreviewCollection(true, dark = true, wishlist = true) }

@Preview(name = "Large text", widthDp = 412, heightDp = 892, fontScale = 1.5f, showBackground = true)
@Composable private fun LargeTextPreview() { PreviewCollection(true) }

@Composable private fun PreviewCollection(populated: Boolean, dark: Boolean = false, wishlist: Boolean = false) {
    val assets = LocalContext.current.assets
    val store = remember {
        CollectionStore(Catalog.read(assets), object : CollectionStorage {
            override fun read(): String? = null
            override fun write(value: String) = Unit
        }).apply {
            load()
            if (populated) {
                addCopy("sv03.5-025", 2, "Normal", "Near mint", "")
                addCopy("sv03.5-001", 1, "Reverse holo", "Near mint", "")
                setWishlist("sv03.5-199", true)
                setWishlist("sv03.5-205", true)
            }
            clearFeedback()
        }
    }
    var tab by remember { mutableStateOf(if (wishlist) Scope.Wishlist else Scope.Collection) }
    val scope = rememberCoroutineScope()
    val controller = remember { CollectionController(store, scope) }
    CollectionTheme(darkTheme = dark) { CollectionScreen(controller, tab) { tab = it } }
}
