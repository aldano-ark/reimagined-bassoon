package com.example.pokemoncollection.collection

import android.content.res.AssetManager
import android.icu.text.BreakIterator
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

public data class Card(val id: String, val name: String, val number: String, val set: String, val printedTotal: Int, val rarity: String, val image: String, val finishes: List<String>) {
    val identity: String get() = "$set · $number/$printedTotal"
}
public data class CopyEntry(val id: String, val cardId: String, val quantity: Int, val finish: String, val condition: String, val note: String, val createdAt: Long)
public data class CollectionState(val copies: List<CopyEntry> = emptyList(), val wishlist: Map<String, Long> = emptyMap()) {
    val totalCopies: Int get() = copies.sumOf { it.quantity }
    val uniqueCards: Int get() = copies.map { it.cardId }.toSet().size
    fun quantity(cardId: String): Int = copies.filter { it.cardId == cardId }.sumOf { it.quantity }
}
public enum class Scope { Collection, Wishlist, Catalog }
public enum class SortOrder(val label: String) { Recent("Recently saved"), Name("Name"), Number("Card number") }
public val copyConditions: List<String> = listOf("Near mint", "Lightly played", "Moderately played", "Heavily played", "Damaged")

public class Catalog(val cards: List<Card>) {
    init { require(cards.isNotEmpty() && cards.map { it.id }.toSet().size == cards.size) }
    fun search(query: String): List<Card> {
        val term = query.trim()
        return cards.filter {
            term.isEmpty() || it.name.contains(term, true) || it.set.contains(term, true) ||
                "Scarlet & Violet–${it.set}".contains(term, true) || it.number.contains(term, true) ||
                (term.toIntOrNull() != null && term.toIntOrNull() == it.number.toIntOrNull())
        }
    }
    companion object {
        fun read(assets: AssetManager): Catalog {
            val array = JSONArray(assets.open("catalog.json").bufferedReader().use { it.readText() })
            return Catalog((0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val finishes = item.getJSONArray("finishes")
                Card(item.getString("id"), item.getString("name"), item.getString("number"), item.getString("set"), item.getInt("printedTotal"), item.getString("rarity"), item.getString("image"), (0 until finishes.length()).map { finishes.getString(it) })
            })
        }
    }
}

/** Missing data is distinct from unreadable data. Writes either replace the file atomically or throw. */
public interface CollectionStorage { fun read(): String?; fun write(value: String) }
public class AtomicCollectionStorage(private val file: File) : CollectionStorage {
    override fun read(): String? = try {
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(Files.readAllBytes(file.toPath()))).toString()
    } catch (_: NoSuchFileException) { null }

    override fun write(value: String) {
        Files.createDirectories(requireNotNull(file.parentFile).toPath())
        val temporary = Files.createTempFile(file.parentFile!!.toPath(), ".collection-", ".tmp")
        try {
            FileOutputStream(temporary.toFile()).use { output ->
                output.write(value.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            // No non-atomic fallback: a filesystem without atomic replacement exposes a retryable error.
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temporary) }
    }
}

/** Feature-owned committed state. The app supplies storage; UI dispatches blocking work on IO. */
public class CollectionStore(val catalog: Catalog, private val storage: CollectionStorage) {
    var state: CollectionState by mutableStateOf(CollectionState())
        private set
    var loaded: Boolean by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var notice: String? by mutableStateOf(null)
        private set
    private var pending: Pair<CollectionState, String>? = null

    @Synchronized fun load(): Boolean = try {
        val restored = storage.read()?.let(::decode) ?: CollectionState()
        state = restored
        loaded = true
        error = null
        pending = null
        true
    } catch (_: Exception) {
        error = "Saved data could not be read. Your data has not been changed. Retry when storage is available."
        notice = null
        false
    }

    @Synchronized fun retry(): Boolean {
        if (!loaded) return load()
        return pending?.let { commit(it.first, it.second) } ?: true
    }

    @Synchronized fun clearFeedback() { if (loaded) error = null; notice = null; pending = null }

    @Synchronized fun addCopy(id: String, quantity: Int, finish: String, condition: String, note: String): Boolean {
        if (!loaded) return false
        val entry = CopyEntry(UUID.randomUUID().toString(), id, quantity, finish, condition, note, System.currentTimeMillis())
        try { validateEntry(entry) } catch (failure: IllegalArgumentException) {
            error = failure.message
            notice = null
            pending = null
            return false
        }
        return commit(state.copy(copies = state.copies + entry), "Copy saved")
    }

    @Synchronized fun setWishlist(id: String, wanted: Boolean): Boolean {
        if (!loaded || catalog.cards.none { it.id == id }) return false
        if (state.wishlist.containsKey(id) == wanted) return true
        val wishlist = if (wanted) state.wishlist + (id to System.currentTimeMillis()) else state.wishlist - id
        return commit(state.copy(wishlist = wishlist), if (wanted) "Added to wishlist" else "Removed from wishlist")
    }

    @Synchronized fun deleteCopy(id: String): Boolean {
        if (!loaded || state.copies.none { it.id == id }) return false
        return commit(state.copy(copies = state.copies.filterNot { it.id == id }), "Copy entry deleted")
    }

    fun cards(scope: Scope, query: String, sort: SortOrder): List<Card> {
        val matches = catalog.search(query).filter { card -> when (scope) {
            Scope.Catalog -> true
            Scope.Collection -> state.quantity(card.id) > 0
            Scope.Wishlist -> state.wishlist.containsKey(card.id)
        } }
        return when (sort) {
            SortOrder.Name -> matches.sortedBy { it.name.lowercase() }
            SortOrder.Number -> matches.sortedBy { it.number.toInt() }
            SortOrder.Recent -> if (scope == Scope.Catalog) matches else matches.sortedByDescending { card ->
                if (scope == Scope.Wishlist) state.wishlist[card.id] ?: 0L
                else state.copies.filter { it.cardId == card.id }.maxOfOrNull { it.createdAt } ?: 0L
            }
        }
    }

    private fun commit(candidate: CollectionState, message: String): Boolean {
        notice = null
        return try {
            storage.write(encode(candidate))
            state = candidate
            pending = null
            error = null
            notice = message
            true
        } catch (_: Exception) {
            pending = candidate to message
            error = "Could not save changes. Your last saved collection is unchanged. Retry to save this change."
            false
        }
    }

    private fun validateEntry(entry: CopyEntry) {
        val card = catalog.cards.find { it.id == entry.cardId }
        require(card != null) { "Choose a card from the example catalog." }
        require(entry.quantity in 1..99) { "Quantity must be between 1 and 99." }
        require(entry.finish in card.finishes) { "Choose an available finish." }
        require(entry.condition in copyConditions) { "Choose a condition." }
        require(noteCharacterCount(entry.note) <= 500) { "Keep the note to 500 characters or fewer." }
        require(entry.createdAt > 0) { "Invalid saved date." }
        UUID.fromString(entry.id)
    }

    private fun encode(value: CollectionState): String = JSONObject().apply {
        put("version", 1)
        put("copies", JSONArray().apply { value.copies.forEach { entry -> put(JSONObject().apply {
            put("id", entry.id); put("cardId", entry.cardId); put("quantity", entry.quantity)
            put("finish", entry.finish); put("condition", entry.condition); put("note", entry.note); put("createdAt", entry.createdAt)
        }) } })
        put("wishlist", JSONArray().apply { value.wishlist.forEach { (cardId, savedAt) -> put(JSONObject().apply {
            put("cardId", cardId); put("savedAt", savedAt)
        }) } })
    }.toString()

    private fun decode(raw: String): CollectionState {
        val tokens = JSONTokener(raw)
        val root = tokens.nextValue() as? JSONObject ?: error("Invalid saved data")
        require(tokens.nextClean() == '\u0000') { "Trailing saved data" }
        require(root.strictLong("version") == 1L) { "Unsupported saved data version." }
        val copyArray = root.getJSONArray("copies")
        val copies = (0 until copyArray.length()).map { index ->
            val item = copyArray.getJSONObject(index)
            val quantity = item.strictLong("quantity")
            require(quantity in 1..99)
            CopyEntry(item.strictString("id"), item.strictString("cardId"), quantity.toInt(), item.strictString("finish"), item.strictString("condition"), item.strictString("note"), item.strictLong("createdAt")).also(::validateEntry)
        }
        require(copies.map { it.id }.toSet().size == copies.size)
        val wishArray = root.getJSONArray("wishlist")
        val wishes = (0 until wishArray.length()).map { index ->
            val item = wishArray.getJSONObject(index)
            val id = item.strictString("cardId")
            val time = item.strictLong("savedAt")
            require(catalog.cards.any { it.id == id } && time > 0)
            id to time
        }
        require(wishes.map { it.first }.toSet().size == wishes.size)
        require(copies.sumOf { it.quantity.toLong() } <= Int.MAX_VALUE)
        return CollectionState(copies, wishes.toMap())
    }
}
private fun JSONObject.strictString(key: String): String = (get(key) as? String) ?: error("Invalid string")
private fun JSONObject.strictLong(key: String): Long {
    val value = get(key)
    require(value is Int || value is Long) { "Invalid integer" }
    return (value as Number).toLong()
}

/** ICU grapheme boundaries match user-visible characters, including emoji sequences and accents. */
internal fun noteCharacterCount(note: String): Int {
    val boundaries = BreakIterator.getCharacterInstance(Locale.ROOT)
    boundaries.setText(note)
    boundaries.first()
    var count = 0
    while (boundaries.next() != BreakIterator.DONE) count++
    return count
}
