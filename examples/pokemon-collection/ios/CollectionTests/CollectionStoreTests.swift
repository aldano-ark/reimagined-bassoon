import XCTest
@testable import CollectionFeature

@MainActor
final class CollectionStoreTests: XCTestCase {
    let pikachu = Card(id: "sv03.5-025", name: "Pikachu", number: "025", set: "151", printedTotal: 165, rarity: "Common", image: "card_025.png", finishes: ["Normal", "Reverse holo"])
    let bulbasaur = Card(id: "sv03.5-001", name: "Bulbasaur", number: "001", set: "151", printedTotal: 165, rarity: "Common", image: "card_001.png", finishes: ["Normal", "Reverse holo"])

    func testEmptySearchAndSortUseActualSavedData() throws {
        let store = CollectionStore(catalog: [pikachu, bulbasaur], storage: MemoryStorage())
        XCTAssertEqual(store.totalCopies, 0)
        XCTAssertEqual(store.uniqueCards, 0)
        XCTAssertEqual(store.snapshot.wishlist.count, 0)
        XCTAssertEqual(store.cards(matching: "  pikACHu ", scope: .catalog).map(\.id), ["sv03.5-025"])
        XCTAssertEqual(store.cards(matching: "25", scope: .catalog).map(\.id), ["sv03.5-025"])
        XCTAssertEqual(store.cards(matching: "151", scope: .catalog).count, 2)
        XCTAssertTrue(store.cards(matching: "missing", scope: .catalog).isEmpty)
        XCTAssertTrue(store.cards(matching: "", scope: .collection).isEmpty)
        try store.add(cardID: pikachu.id, quantity: 2, finish: "Normal", condition: .nearMint, note: "")
        try store.add(cardID: bulbasaur.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "")
        XCTAssertEqual(store.cards(scope: .collection, sort: .recent).map(\.id), [bulbasaur.id, pikachu.id])
        XCTAssertEqual(store.cards(scope: .collection, sort: .number).map(\.id), [bulbasaur.id, pikachu.id])
        XCTAssertEqual(store.cards(scope: .collection, sort: .name).map(\.id), [bulbasaur.id, pikachu.id])
    }

    func testAtomicFileRoundTripPreservesEntriesAndIndependentWishlist() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storage = FileCollectionStorage(url: directory.appendingPathComponent("collection.json"))
        let store = CollectionStore(catalog: [pikachu], storage: storage)
        try store.add(cardID: pikachu.id, quantity: 2, finish: "Reverse holo", condition: .lightlyPlayed, note: "Gift")
        try store.add(cardID: pikachu.id, quantity: 3, finish: "Normal", condition: .nearMint, note: "Binder")
        try store.setWishlist(cardID: pikachu.id, included: true)
        try store.setWishlist(cardID: pikachu.id, included: true)
        let reopened = CollectionStore(catalog: [pikachu], storage: storage)
        XCTAssertNil(reopened.loadError)
        XCTAssertEqual(reopened.totalCopies, 5)
        XCTAssertEqual(reopened.uniqueCards, 1)
        XCTAssertEqual(reopened.snapshot.wishlist.count, 1)
        let first = try XCTUnwrap(reopened.snapshot.copies.first)
        XCTAssertEqual(first.note, "Gift")
        XCTAssertEqual(first.finish, "Reverse holo")
        XCTAssertEqual(first.condition, .lightlyPlayed)
        try reopened.delete(entryID: first.id)
        XCTAssertEqual(reopened.totalCopies, 3)
        XCTAssertEqual(reopened.snapshot.copies.first?.note, "Binder")
        try reopened.delete(entryID: XCTUnwrap(reopened.snapshot.copies.first).id)
        XCTAssertEqual(reopened.totalCopies, 0)
        XCTAssertTrue(reopened.isWishlisted(pikachu.id))
        XCTAssertEqual(CollectionStore(catalog: [pikachu], storage: storage).totalCopies, 0)
    }

    func testValidationCannotChangeCommittedBytes() throws {
        let storage = MemoryStorage()
        let store = CollectionStore(catalog: [pikachu], storage: storage)
        try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "Original")
        let committed = storage.bytes
        for quantity in [0, 100] {
            XCTAssertThrowsError(try store.add(cardID: pikachu.id, quantity: quantity, finish: "Normal", condition: .nearMint, note: ""))
        }
        XCTAssertThrowsError(try store.add(cardID: pikachu.id, quantity: 1, finish: "Holo", condition: .nearMint, note: ""))
        XCTAssertThrowsError(try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: String(repeating: "x", count: 501)))
        XCTAssertThrowsError(try store.add(cardID: "unknown", quantity: 1, finish: "Normal", condition: .nearMint, note: ""))
        XCTAssertEqual(storage.bytes, committed)
        XCTAssertEqual(store.totalCopies, 1)
    }

    func testFailedWriteKeepsCommittedStateAndCanRetry() throws {
        let storage = MemoryStorage()
        let store = CollectionStore(catalog: [pikachu], storage: storage)
        try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "")
        let committed = storage.bytes
        storage.failWrite = true
        XCTAssertThrowsError(try store.add(cardID: pikachu.id, quantity: 2, finish: "Normal", condition: .nearMint, note: ""))
        XCTAssertThrowsError(try store.setWishlist(cardID: pikachu.id, included: true))
        XCTAssertThrowsError(try store.delete(entryID: XCTUnwrap(store.snapshot.copies.first).id))
        XCTAssertEqual(store.totalCopies, 1)
        XCTAssertFalse(store.isWishlisted(pikachu.id))
        XCTAssertEqual(storage.bytes, committed)
        storage.failWrite = false
        try store.add(cardID: pikachu.id, quantity: 2, finish: "Normal", condition: .nearMint, note: "Retry")
        XCTAssertEqual(store.totalCopies, 3)
    }

    func testCorruptUnsupportedAndInvalidDataArePreservedUntilReadable() throws {
        let badDocuments = [Data("broken".utf8), Data(#"{"schemaVersion":99,"copies":[],"wishlist":[]}"#.utf8), Data(#"{"schemaVersion":1,"copies":[],"wishlist":[{"cardID":"missing","addedAt":0}]}"#.utf8)]
        for bytes in badDocuments {
            let storage = MemoryStorage(bytes: bytes)
            let store = CollectionStore(catalog: [pikachu], storage: storage)
            XCTAssertNotNil(store.loadError)
            XCTAssertThrowsError(try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: ""))
            XCTAssertEqual(storage.bytes, bytes)
            storage.bytes = nil
            store.reload()
            XCTAssertNil(store.loadError)
            try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "")
            XCTAssertEqual(store.totalCopies, 1)
        }
    }

    func testDecodedCopyValidationPreservesInvalidDocuments() throws {
        let storage = MemoryStorage()
        let store = CollectionStore(catalog: [pikachu], storage: storage)
        try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "")
        let valid = try XCTUnwrap(storage.bytes)
        let original = try XCTUnwrap(JSONSerialization.jsonObject(with: valid) as? [String: Any])
        let entries = try XCTUnwrap(original["copies"] as? [[String: Any]])
        for (field, value) in [("quantity", 0 as Any), ("finish", "Holo" as Any), ("condition", "Unrecognized" as Any), ("note", String(repeating: "x", count: 501) as Any), ("cardID", "missing" as Any)] {
            var document = original
            var invalidEntry = try XCTUnwrap(entries.first)
            invalidEntry[field] = value
            document["copies"] = [invalidEntry]
            let invalid = try JSONSerialization.data(withJSONObject: document)
            storage.bytes = invalid
            let reopened = CollectionStore(catalog: [pikachu], storage: storage)
            XCTAssertNotNil(reopened.loadError, field)
            XCTAssertThrowsError(try reopened.setWishlist(cardID: pikachu.id, included: true))
            XCTAssertEqual(storage.bytes, invalid)
        }
    }

    func testMaximumValidQuantityAndNoteSurviveReload() throws {
        let storage = MemoryStorage()
        let store = CollectionStore(catalog: [pikachu], storage: storage)
        let note = String(repeating: "é", count: 500)
        try store.add(cardID: pikachu.id, quantity: 99, finish: "Reverse holo", condition: .damaged, note: note)
        let reopened = CollectionStore(catalog: [pikachu], storage: storage)
        XCTAssertNil(reopened.loadError)
        XCTAssertEqual(reopened.totalCopies, 99)
        XCTAssertEqual(reopened.snapshot.copies.first?.note, note)
        XCTAssertEqual(reopened.snapshot.copies.first?.condition, .damaged)
    }

    func testUnicodeNoteLimitUsesUserVisibleCharacters() throws {
        for character in ["😀", "👩‍👩‍👧‍👦", "e\u{301}"] {
            let storage = MemoryStorage()
            let store = CollectionStore(catalog: [pikachu], storage: storage)
            let validNote = String(repeating: character, count: 500)
            try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: validNote)
            let committed = storage.bytes
            XCTAssertThrowsError(try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: String(repeating: character, count: 501)))
            XCTAssertEqual(storage.bytes, committed)
            let reopened = CollectionStore(catalog: [pikachu], storage: storage)
            XCTAssertEqual(reopened.snapshot.copies.first?.note, validNote)
            XCTAssertEqual(reopened.totalCopies, 1)
        }
    }

    func testSavedSortOptionsHaveDistinctOrdersAndDoNotWriteData() throws {
        let charizard = Card(id: "sv03.5-199", name: "Charizard ex", number: "199", set: "151", printedTotal: 165, rarity: "Special illustration rare", image: "card_199.png", finishes: ["Holo"])
        let storage = MemoryStorage()
        let store = CollectionStore(catalog: [pikachu, bulbasaur, charizard], storage: storage)
        try store.add(cardID: pikachu.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "")
        try store.add(cardID: bulbasaur.id, quantity: 1, finish: "Normal", condition: .nearMint, note: "")
        try store.add(cardID: charizard.id, quantity: 1, finish: "Holo", condition: .nearMint, note: "")
        try store.setWishlist(cardID: pikachu.id, included: true)
        try store.setWishlist(cardID: bulbasaur.id, included: true)
        try store.setWishlist(cardID: charizard.id, included: true)
        let committed = storage.bytes
        for scope in [CardScope.collection, .wishlist] {
            XCTAssertEqual(store.cards(scope: scope, sort: .recent).map(\.id), [charizard.id, bulbasaur.id, pikachu.id])
            XCTAssertEqual(store.cards(scope: scope, sort: .name).map(\.id), [bulbasaur.id, charizard.id, pikachu.id])
            XCTAssertEqual(store.cards(scope: scope, sort: .number).map(\.id), [bulbasaur.id, pikachu.id, charizard.id])
            XCTAssertEqual(store.cards(matching: "25", scope: scope).map(\.id), [pikachu.id])
            XCTAssertTrue(store.cards(matching: "missing", scope: scope).isEmpty)
        }
        XCTAssertEqual(storage.bytes, committed)
    }

    func testUnreadableStorageBlocksMutationsAndRetryLoadsCommittedData() throws {
        let storage = MemoryStorage()
        let store = CollectionStore(catalog: [pikachu], storage: storage)
        try store.setWishlist(cardID: pikachu.id, included: true)
        storage.failRead = true
        let reopened = CollectionStore(catalog: [pikachu], storage: storage)
        XCTAssertNotNil(reopened.loadError)
        XCTAssertThrowsError(try reopened.setWishlist(cardID: pikachu.id, included: false))
        storage.failRead = false
        reopened.reload()
        XCTAssertNil(reopened.loadError)
        XCTAssertTrue(reopened.isWishlisted(pikachu.id))
    }
}

@MainActor
private final class MemoryStorage: CollectionStorage {
    var bytes: Data?
    var failWrite = false
    var failRead = false
    init(bytes: Data? = nil) { self.bytes = bytes }
    func read() throws -> Data? {
        if failRead { throw CocoaError(.fileReadNoPermission) }
        return bytes
    }
    func write(_ data: Data) throws {
        if failWrite { throw CocoaError(.fileWriteNoPermission) }
        bytes = data
    }
}
