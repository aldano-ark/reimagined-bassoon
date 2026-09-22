import Foundation
import Observation

public struct Card: Codable, Identifiable, Hashable, Sendable {
    public let id: String
    public let name: String
    public let number: String
    public let set: String
    public let printedTotal: Int
    public let rarity: String
    public let image: String
    public let finishes: [String]
}
public enum CardCondition: String, Codable, CaseIterable, Sendable {
    case nearMint = "Near mint", lightlyPlayed = "Lightly played", moderatelyPlayed = "Moderately played", heavilyPlayed = "Heavily played", damaged = "Damaged"
}
public struct CopyEntry: Codable, Identifiable, Equatable, Sendable {
    public let id: UUID
    public let cardID: String
    public let quantity: Int
    public let finish: String
    public let condition: CardCondition
    public let note: String
    public let addedAt: Date
}
public struct WishlistEntry: Codable, Equatable, Sendable {
    public let cardID: String
    public let addedAt: Date
}
public struct CollectionSnapshot: Codable, Equatable, Sendable {
    var schemaVersion = 1
    public var copies: [CopyEntry] = []
    public var wishlist: [WishlistEntry] = []
}
public enum CardScope: Sendable { case collection, wishlist, catalog }
public enum CardSort: String, CaseIterable, Sendable { case recent = "Recently added", name = "Name", number = "Card number" }
@MainActor public protocol CollectionStorage {
    func read() throws -> Data?
    func write(_ data: Data) throws
}
/// Synchronous, main-actor storage serializes this small local document with UI mutations.
/// The replacement is atomic: a failed write cannot replace the committed document.
@MainActor public final class FileCollectionStorage: CollectionStorage {
    private let url: URL
    public init(url: URL) { self.url = url }
    public func read() throws -> Data? {
        do { return try Data(contentsOf: url) }
        catch let error as CocoaError where error.code == .fileReadNoSuchFile { return nil }
    }
    public func write(_ data: Data) throws {
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try data.write(to: url, options: .atomic)
    }
}

public struct CollectionError: LocalizedError, Sendable {
    public let message: String
    public var errorDescription: String? { message }
    init(_ message: String) { self.message = message }
}

public enum CardCatalog {
    public static func load(from url: URL) throws -> [Card] {
        let cards = try JSONDecoder().decode([Card].self, from: Data(contentsOf: url))
        guard !cards.isEmpty, Set(cards.map(\.id)).count == cards.count,
              cards.allSatisfy({ !$0.id.isEmpty && !$0.name.isEmpty && Int($0.number) != nil && !$0.set.isEmpty && $0.printedTotal > 0 && !$0.finishes.isEmpty && Set($0.finishes).count == $0.finishes.count && Set($0.finishes).isSubset(of: ["Normal", "Reverse holo", "Holo"]) && URL(fileURLWithPath: $0.image).lastPathComponent == $0.image }) else {
            throw CollectionError("The bundled card catalog is invalid.")
        }
        return cards
    }
}

@Observable @MainActor public final class CollectionStore {
    public let catalog: [Card]
    public private(set) var snapshot = CollectionSnapshot()
    public private(set) var loadError: String?
    private let storage: any CollectionStorage

    public init(catalog: [Card], storage: any CollectionStorage) {
        self.catalog = catalog
        self.storage = storage
        reload()
    }
    public var totalCopies: Int { snapshot.copies.reduce(0) { $0 + $1.quantity } }
    public var uniqueCards: Int { Set(snapshot.copies.map(\.cardID)).count }
    public func copies(of id: String) -> [CopyEntry] { snapshot.copies.filter { $0.cardID == id } }
    public func quantity(of id: String) -> Int { copies(of: id).reduce(0) { $0 + $1.quantity } }
    public func isWishlisted(_ id: String) -> Bool { snapshot.wishlist.contains { $0.cardID == id } }

    public func reload() {
        do {
            let loaded = try storage.read().map { try JSONDecoder().decode(CollectionSnapshot.self, from: $0) } ?? CollectionSnapshot()
            try validate(loaded)
            snapshot = loaded
            loadError = nil
        } catch {
            loadError = "Your saved collection could not be read. The existing data has been preserved. \(error.localizedDescription)"
        }
    }

    public func cards(matching query: String = "", scope: CardScope, sort: CardSort = .recent) -> [Card] {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let matches = catalog.filter { card in
            let inScope = scope == .catalog || (scope == .collection ? quantity(of: card.id) > 0 : isWishlisted(card.id))
            let numberMatches = Int(term).map { $0 == Int(card.number) } ?? card.number.localizedCaseInsensitiveContains(term)
            return inScope && (term.isEmpty || card.name.localizedCaseInsensitiveContains(term) || card.set.localizedCaseInsensitiveContains(term) || "Scarlet & Violet—\(card.set)".localizedCaseInsensitiveContains(term) || numberMatches)
        }
        if scope == .catalog && sort == .recent { return matches }
        return matches.sorted { left, right in
            switch sort {
            case .name: return left.name.localizedStandardCompare(right.name) == .orderedAscending
            case .number: return (Int(left.number) ?? 0) < (Int(right.number) ?? 0)
            case .recent:
                let a = latestDate(for: left.id, scope: scope)
                let b = latestDate(for: right.id, scope: scope)
                return a == b ? left.id < right.id : a > b
            }
        }
    }

    public func add(cardID: String, quantity: Int, finish: String, condition: CardCondition, note: String) throws {
        let entry = CopyEntry(id: UUID(), cardID: cardID, quantity: quantity, finish: finish, condition: condition, note: note, addedAt: Date())
        try validate(entry)
        var next = snapshot
        next.copies.append(entry)
        try commit(next)
    }

    /// Setting membership is idempotent and never changes owned copies.
    public func setWishlist(cardID: String, included: Bool) throws {
        try requireReadable()
        guard catalog.contains(where: { $0.id == cardID }) else { throw CollectionError("Unknown card.") }
        guard isWishlisted(cardID) != included else { return }
        var next = snapshot
        next.wishlist.removeAll { $0.cardID == cardID }
        if included { next.wishlist.append(WishlistEntry(cardID: cardID, addedAt: Date())) }
        try commit(next)
    }

    public func delete(entryID: UUID) throws {
        var next = snapshot
        guard next.copies.contains(where: { $0.id == entryID }) else { throw CollectionError("This copy entry no longer exists.") }
        next.copies.removeAll { $0.id == entryID }
        try commit(next)
    }

    private func commit(_ next: CollectionSnapshot) throws {
        try requireReadable()
        try validate(next)
        do { try storage.write(JSONEncoder().encode(next)) }
        catch { throw CollectionError("Could not save your collection. Your previous saved collection is unchanged. Try again. \(error.localizedDescription)") }
        snapshot = next
    }
    private func requireReadable() throws {
        guard loadError == nil else { throw CollectionError("Retry loading your collection before making changes.") }
    }
    private func validate(_ entry: CopyEntry) throws {
        guard let card = catalog.first(where: { $0.id == entry.cardID }) else { throw CollectionError("Unknown card in saved collection.") }
        guard (1...99).contains(entry.quantity) else { throw CollectionError("Quantity must be from 1 to 99.") }
        guard card.finishes.contains(entry.finish) else { throw CollectionError("Choose a finish available for this card.") }
        guard entry.note.count <= 500 else { throw CollectionError("Notes must be 500 characters or fewer.") }
        guard entry.addedAt.timeIntervalSinceReferenceDate.isFinite else { throw CollectionError("Invalid saved date.") }
    }
    private func validate(_ document: CollectionSnapshot) throws {
        guard document.schemaVersion == 1 else { throw CollectionError("This collection uses an unsupported storage version.") }
        guard Set(document.copies.map(\.id)).count == document.copies.count,
              Set(document.wishlist.map(\.cardID)).count == document.wishlist.count else { throw CollectionError("The saved collection has duplicate entries.") }
        for entry in document.copies { try validate(entry) }
        for entry in document.wishlist {
            guard catalog.contains(where: { $0.id == entry.cardID }), entry.addedAt.timeIntervalSinceReferenceDate.isFinite else { throw CollectionError("Invalid saved wishlist entry.") }
        }
    }
    private func latestDate(for id: String, scope: CardScope) -> Date {
        if scope == .wishlist { return snapshot.wishlist.first { $0.cardID == id }?.addedAt ?? .distantPast }
        return copies(of: id).map(\.addedAt).max() ?? .distantPast
    }
}
