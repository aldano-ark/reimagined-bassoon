package com.example.pokemoncollection.collection

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativetemplate.designsystem.components.*
import com.example.nativetemplate.designsystem.theme.DSSpacing
import com.example.nativetemplate.designsystem.theme.DSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable public fun CollectionTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    DSTheme(darkTheme = darkTheme, dynamicColor = false) {
        val colors = if (darkTheme) darkColorScheme(
            primary = Color(0xFF92D5B3), onPrimary = Color(0xFF003824),
            primaryContainer = Color(0xFF194D39), onPrimaryContainer = Color(0xFFBBF2D5),
            secondary = Color(0xFF92D5B3), onSecondary = Color(0xFF003824),
            secondaryContainer = Color(0xFF244C3B), onSecondaryContainer = Color(0xFFC6EAD6),
            tertiary = Color(0xFFBBCBC0), onTertiary = Color(0xFF1D2922),
            tertiaryContainer = Color(0xFF2D3D33), onTertiaryContainer = Color(0xFFE0E9E2),
            outline = Color(0xFF87998C), outlineVariant = Color(0xFF3F5145),
            surfaceContainerLowest = Color(0xFF0C120F), surfaceContainerLow = Color(0xFF18221C),
            surfaceContainerHigh = Color(0xFF253129), surfaceContainerHighest = Color(0xFF303D33),
            surfaceDim = Color(0xFF111814), surfaceBright = Color(0xFF344039),
            background = Color(0xFF111814), surface = Color(0xFF111814),
            surfaceContainer = Color(0xFF1D2922), onSurface = Color(0xFFE0E9E2),
            onSurfaceVariant = Color(0xFFBBCBC0),
        ) else lightColorScheme(
            primary = Color(0xFF236B51), onPrimary = Color.White,
            primaryContainer = Color(0xFFE6F3EB), onPrimaryContainer = Color(0xFF236B51),
            secondary = Color(0xFF236B51), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE6F3EB), onSecondaryContainer = Color(0xFF236B51),
            tertiary = Color(0xFF5C6D65), onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE8EEE9), onTertiaryContainer = Color(0xFF182B24),
            outline = Color(0xFF819087), outlineVariant = Color(0xFFD8E2DB),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8FAF8),
            surfaceContainerHigh = Color(0xFFEDF2EE), surfaceContainerHighest = Color(0xFFE5ECE6),
            surfaceDim = Color(0xFFDEE6E0), surfaceBright = Color.White,
            background = Color.White, surface = Color.White,
            surfaceContainer = Color(0xFFF4F7F5), onSurface = Color(0xFF182B24),
            onSurfaceVariant = Color(0xFF5C6D65),
        )
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@Composable public fun CollectionScreen(controller: CollectionController, selectedTab: Scope, onTabChange: (Scope) -> Unit) {
    val store = controller.store
    var catalogOpen by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = controller.busy
    val selectedCard = store.catalog.cards.find { it.id == selectedId }
    fun perform(action: () -> Boolean) { controller.perform(action) }
    LaunchedEffect(store.notice) {
        when (store.notice) {
            "Copy saved" -> adding = false
            "Copy entry deleted" -> deletingId = null
        }
    }
    fun back() {
        if (busy) return
        store.clearFeedback()
        when { adding -> adding = false; selectedId != null -> selectedId = null; else -> catalogOpen = false }
    }
    BackHandler(enabled = catalogOpen || selectedId != null || adding) { back() }
    val title = when {
        adding -> "Add a copy"
        selectedCard != null -> "Card details"
        catalogOpen -> "Find a card"
        selectedTab == Scope.Wishlist -> "Wishlist"
        else -> "Collection"
    }
    val inTabs = !catalogOpen && selectedCard == null && !adding
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = DSSpacing.xl, vertical = DSSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f).semantics { heading() }, style = if (inTabs || catalogOpen) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge)
                    if (!inTabs) TextButton(onClick = { back() }, enabled = !busy) { Text(if (adding) "Cancel" else if (selectedCard != null) "Back" else "Close") }
                }
                if (inTabs) {
                    Text(if (selectedTab == Scope.Collection) "${cardCount(store.state.uniqueCards)} · ${copyCount(store.state.totalCopies)}" else "${cardCount(store.state.wishlist.size)} you're looking for",
                        Modifier.padding(start = DSSpacing.xl, end = DSSpacing.xl, bottom = DSSpacing.lg), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryTabRow(selectedTabIndex = if (selectedTab == Scope.Collection) 0 else 1) {
                        listOf(Scope.Collection, Scope.Wishlist).forEach { tab ->
                            Tab(selected = selectedTab == tab, onClick = { store.clearFeedback(); onTabChange(tab) }, text = { Text(tab.name) }, modifier = Modifier.testTag("tab-${tab.name.lowercase()}"))
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (store.loaded && !adding) Column(Modifier.navigationBarsPadding().padding(horizontal = DSSpacing.xl, vertical = DSSpacing.md), verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                if (selectedCard != null) {
                    DSButton("Add a copy", { store.clearFeedback(); adding = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                    val wished = store.state.wishlist.containsKey(selectedCard.id)
                    DSButton(if (wished) "Remove from wishlist" else "Add to wishlist", { perform({ store.setWishlist(selectedCard.id, !wished) }) }, modifier = Modifier.fillMaxWidth(), intent = DSButtonIntent.Secondary, enabled = !busy)
                } else if (!catalogOpen) {
                    val empty = if (selectedTab == Scope.Collection) store.state.uniqueCards == 0 else store.state.wishlist.isEmpty()
                    DSButton(if (selectedTab == Scope.Wishlist) "Find cards" else if (empty) "Find your first card" else "Add cards", { store.clearFeedback(); catalogOpen = true }, modifier = Modifier.fillMaxWidth().testTag("browse"), enabled = !busy)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!store.loaded) {
                Box(Modifier.fillMaxSize().padding(DSSpacing.xl), contentAlignment = Alignment.Center) {
                    DSStatusView(if (store.error == null) "Opening your collection" else "Could not open your collection", description = store.error ?: "Opening saved cards…", kind = if (store.error == null) DSStatusKind.Neutral else DSStatusKind.Error) {
                        if (store.error != null) DSButton("Retry", { perform({ store.retry() }) }, loading = busy)
                        else CircularProgressIndicator()
                    }
                }
            } else {
                store.error?.let { message ->
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = DSSpacing.xl, vertical = DSSpacing.sm)) {
                            Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { perform { store.retry() } }, enabled = !busy) { Text("Retry") }
                        }
                    }
                }
                store.notice?.let { message ->
                    Text(message, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = DSSpacing.xl, vertical = DSSpacing.sm).semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                when {
                    adding && selectedCard != null -> AddCopyForm(selectedCard, store.state.quantity(selectedCard.id), busy, onDraftChange = store::clearFeedback) { quantity, finish, condition, note ->
                        perform { store.addCopy(selectedCard.id, quantity, finish, condition, note) }
                    }
                    selectedCard != null -> Details(selectedCard, store.state, onDelete = { deletingId = it }, enabled = !busy)
                    else -> Gallery(store, if (catalogOpen) Scope.Catalog else selectedTab, onCard = { store.clearFeedback(); selectedId = it })
                }
            }
        }
    }
    deletingId?.let { entryId ->
        val entry = store.state.copies.find { it.id == entryId }
        if (entry != null) AlertDialog(
            onDismissRequest = { if (!busy) deletingId = null },
            title = { Text("Delete this copy entry?") },
            text = { Text("This removes ${entry.quantity} ${if (entry.quantity == 1) "copy" else "copies"} from your collection. Your wishlist stays saved.") },
            confirmButton = { TextButton(onClick = { perform { store.deleteCopy(entryId) } }, enabled = !busy) { Text("Delete copies", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deletingId = null }, enabled = !busy) { Text("Cancel") } },
        )
    }
}

@Composable private fun Gallery(store: CollectionStore, scope: Scope, onCard: (String) -> Unit) {
    var query by rememberSaveable(scope) { mutableStateOf("") }
    var sortName by rememberSaveable(scope) { mutableStateOf(SortOrder.Recent.name) }
    var sortOpen by remember { mutableStateOf(false) }
    val sort = SortOrder.valueOf(sortName)
    val cards = store.cards(scope, query, sort)
    val emptyScope = when(scope) { Scope.Collection -> store.state.uniqueCards == 0; Scope.Wishlist -> store.state.wishlist.isEmpty(); Scope.Catalog -> false }
    if (emptyScope) {
        Box(Modifier.fillMaxSize().padding(horizontal = DSSpacing.xl).padding(top = DSSpacing.sm)) { EmptyCollection(scope, store.catalog) }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (scope == Scope.Catalog || LocalDensity.current.fontScale > 1.3f) 1 else 2),
        modifier = Modifier.fillMaxSize().padding(horizontal = DSSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(DSSpacing.md),
        verticalArrangement = Arrangement.spacedBy(if (scope == Scope.Catalog) DSSpacing.sm else 20.dp),
        contentPadding = PaddingValues(top = DSSpacing.sm, bottom = DSSpacing.xl),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                DSTextField(query, { query = it }, if (scope == Scope.Catalog) "Search the card catalog" else "Search your ${scope.name.lowercase()}", Modifier.fillMaxWidth().testTag("search"), singleLine = true)
                if (scope == Scope.Catalog) {
                    Text("Scarlet & Violet—151", Modifier.padding(top = DSSpacing.lg), style = MaterialTheme.typography.titleLarge)
                    Text("Seven-card example catalog · ${cards.size} ${if (cards.size == 1) "result" else "results"}", Modifier.padding(vertical = DSSpacing.sm), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else Box {
                    TextButton(onClick = { sortOpen = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("sort")) { Text(if (sort == SortOrder.Recent && scope == Scope.Collection) "Recently added" else sort.label) }
                    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) { SortOrder.entries.forEach { order ->
                        DropdownMenuItem(text = { Text(order.label) }, onClick = { sortName = order.name; sortOpen = false })
                    } }
                }
            }
        }
        if (cards.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            DSStatusView("No cards found", description = "Try another name, set, or printed card number.")
        }
        items(cards, key = { it.id }) { card ->
            if (scope == Scope.Catalog) Column {
                Row(Modifier.fillMaxWidth().clickable { onCard(card.id) }.testTag("card-${card.id}").padding(vertical = DSSpacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DSSpacing.lg)) {
                    Artwork(card, Modifier.width(64.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DSSpacing.xs)) {
                        Text(card.name, style = MaterialTheme.typography.titleMedium)
                        Text(card.identity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(status(card.id, store.state, scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("View", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            } else Column(Modifier.clickable { onCard(card.id) }.testTag("card-${card.id}"), verticalArrangement = Arrangement.spacedBy(DSSpacing.xs)) {
                Artwork(card, Modifier.fillMaxWidth().padding(bottom = DSSpacing.xs))
                Text(card.name, style = MaterialTheme.typography.titleSmall)
                Text(card.identity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status(card.id, store.state, scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable private fun EmptyCollection(scope: Scope, catalog: Catalog) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = DSSpacing.xl), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(DSSpacing.xl), horizontalArrangement = Arrangement.spacedBy(DSSpacing.md, Alignment.CenterHorizontally)) {
            catalog.cards.take(2).forEach { Artwork(it, Modifier.width(104.dp)) }
        }
        Spacer(Modifier.height(DSSpacing.xl))
        DSStatusView(if (scope == Scope.Collection) "Every collection starts with one card." else "Keep your next finds here.", description = if (scope == Scope.Collection) "Find a card you own and add your first copy. Your personal collection grows from here." else "Find a card and add it to your wishlist. You can own a card and keep it on your wishlist.")
    }
}

@Composable private fun Details(card: Card, state: CollectionState, onDelete: (String) -> Unit, enabled: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = DSSpacing.xl), verticalArrangement = Arrangement.spacedBy(DSSpacing.lg), contentPadding = PaddingValues(top = DSSpacing.sm, bottom = DSSpacing.xl)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(DSSpacing.lg), contentAlignment = Alignment.Center) {
                Artwork(card, Modifier.width(198.dp))
            }
        }
        item {
            Text(card.name, style = MaterialTheme.typography.headlineSmall)
            Text("Scarlet & Violet—${card.identity}", Modifier.padding(top = DSSpacing.xs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(card.rarity, Modifier.padding(top = DSSpacing.md), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(if (state.quantity(card.id) > 0) "In your collection · ${copyCount(state.quantity(card.id))} owned" else "Not in your collection", Modifier.padding(top = DSSpacing.sm))
        }
        item { Text("Your copies", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
        val entries = state.copies.filter { it.cardId == card.id }
        if (entries.isEmpty()) item { Text("No copies added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(entries, key = { it.id }) { entry ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(DSSpacing.lg), verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
                Text("${entry.quantity} ${if (entry.quantity == 1) "copy" else "copies"} · ${entry.finish}", fontWeight = FontWeight.Medium)
                Text(entry.condition, style = MaterialTheme.typography.bodyMedium)
                if (entry.note.isNotEmpty()) Text(entry.note, style = MaterialTheme.typography.bodyMedium)
                DSButton("Delete entry", { onDelete(entry.id) }, modifier = Modifier.testTag("delete-${entry.id}"), intent = DSButtonIntent.Destructive, enabled = enabled)
            }
        }
    }
}

@Composable private fun AddCopyForm(card: Card, owned: Int, busy: Boolean, onDraftChange: () -> Unit, onSave: (Int, String, String, String) -> Unit) {
    var quantity by rememberSaveable(card.id) { mutableStateOf("1") }
    var finish by rememberSaveable(card.id) { mutableStateOf(card.finishes.first()) }
    var condition by rememberSaveable(card.id) { mutableStateOf(copyConditions.first()) }
    var note by rememberSaveable(card.id) { mutableStateOf("") }
    var conditionsOpen by remember { mutableStateOf(false) }
    val amount = quantity.toIntOrNull()
    val noteLength = noteCharacterCount(note)
    val valid = amount != null && amount in 1..99 && noteLength <= 500
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = DSSpacing.xl, vertical = DSSpacing.sm), verticalArrangement = Arrangement.spacedBy(DSSpacing.lg)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(DSSpacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DSSpacing.lg)) {
                Artwork(card, Modifier.width(64.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DSSpacing.xs)) {
                    Text(card.name, style = MaterialTheme.typography.titleLarge)
                    Text(card.identity, style = MaterialTheme.typography.bodySmall)
                    Text("${copyCount(owned)} already owned", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            DSTextField(quantity, { quantity = it; onDraftChange() }, "Quantity", Modifier.fillMaxWidth().testTag("quantity"), enabled = !busy, errorText = if (amount == null || amount !in 1..99) "Enter a quantity from 1 to 99." else null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Text("Finish", fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DSSpacing.sm)) { card.finishes.forEach { available ->
                FilterChip(selected = finish == available, onClick = { finish = available; onDraftChange() }, label = { Text(available) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp))
            } }
            Text("Condition", fontWeight = FontWeight.Medium)
            Box {
                OutlinedButton(onClick = { conditionsOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("condition"), enabled = !busy) { Text(condition) }
                DropdownMenu(expanded = conditionsOpen, onDismissRequest = { conditionsOpen = false }) { copyConditions.forEach { choice ->
                    DropdownMenuItem(text = { Text(choice) }, onClick = { condition = choice; conditionsOpen = false; onDraftChange() })
                } }
            }
            DSTextField(note, { note = it; onDraftChange() }, "Note (optional)", Modifier.fillMaxWidth().testTag("note"), enabled = !busy, supportingText = "$noteLength/500 characters", errorText = if (noteLength > 500) "Keep the note to 500 characters or fewer." else null)
            Text("Your collection will have ${copyCount(owned + (amount?.takeIf { it in 1..99 } ?: 0))} of this card.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DSButton(if (amount == 1) "Add 1 copy" else "Add ${amount ?: 0} copies", { amount?.let { onSave(it, finish, condition, note) } }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DSSpacing.xl, vertical = DSSpacing.md).testTag("save-copy"), enabled = valid && !busy, loading = busy)
    }
}

@Composable internal fun Artwork(card: Card, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val bitmap by produceState<ImageBitmap?>(null, card.image) {
        value = withContext(Dispatchers.IO) { assets.open(card.image).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }
    }
    Box(modifier.aspectRatio(0.716f).clip(RoundedCornerShape(8.dp))) {
        bitmap?.let { Image(it, contentDescription = "${card.name}, ${card.number}/${card.printedTotal} card artwork", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
    }
}

private fun status(id: String, state: CollectionState, scope: Scope): String {
    val quantity = state.quantity(id)
    val copies = "$quantity ${if (quantity == 1) "copy" else "copies"}"
    return when {
        scope == Scope.Wishlist -> if (quantity > 0) "On wishlist · $copies owned" else "On wishlist"
        quantity > 0 -> if (scope == Scope.Catalog) "$copies owned" else copies
        state.wishlist.containsKey(id) -> "On wishlist"
        else -> "Not owned"
    }
}

private fun copyCount(count: Int): String = "$count ${if (count == 1) "copy" else "copies"}"
private fun cardCount(count: Int): String = "$count ${if (count == 1) "card" else "cards"}"
