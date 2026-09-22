import DesignSystem
import SwiftUI
import UIKit

/// One saved-card destination. The app assembles tabs and their navigation stacks.
public struct SavedCardsView: View {
    private let store: CollectionStore
    private let scope: CardScope
    private let artworkDirectory: URL
    @Environment(\.dynamicTypeSize) private var textSize
    @State private var query = ""
    @State private var sort = CardSort.recent
    @State private var findingCard = false

    public init(store: CollectionStore, scope: CardScope, artworkDirectory: URL) {
        self.store = store
        self.scope = scope
        self.artworkDirectory = artworkDirectory
    }
    private var isCollection: Bool { scope == .collection }
    private var title: String { isCollection ? "Collection" : "Wishlist" }
    private var savedCards: [Card] { store.cards(scope: scope) }
    private var results: [Card] { store.cards(matching: query, scope: scope, sort: sort) }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.lg) {
                if let error = store.loadError {
                    DSStatusView("Collection unavailable", description: error, kind: .error) {
                        CollectionActionButton("Retry", action: store.reload).tint(Color("PrimaryButton"))
                    }
                } else {
                    Text(isCollection ? "\(store.uniqueCards) \(store.uniqueCards == 1 ? "card" : "cards") · \(copyCount(store.totalCopies))" : "\(store.snapshot.wishlist.count) \(store.snapshot.wishlist.count == 1 ? "card" : "cards") you’re looking for")
                        .foregroundStyle(.secondary)
                    if savedCards.isEmpty {
                        emptyState
                    } else {
                        SearchField(title: isCollection ? "Search your collection" : "Search your wishlist", query: $query)
                        HStack {
                            Text(sort == .recent && !isCollection ? "Recently saved" : sort.rawValue).font(.subheadline.weight(.semibold))
                            Spacer()
                            Menu {
                                Picker("Sort cards", selection: $sort) {
                                    ForEach(CardSort.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                                }
                            } label: {
                                Label("Sort cards", systemImage: "slider.horizontal.3").labelStyle(.iconOnly).frame(minWidth: 44, minHeight: 44)
                            }
                        }
                        if results.isEmpty {
                            DSStatusView("No cards found", description: "Try a Pokémon name, set name, or printed card number.")
                        } else {
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: DSSpacing.md, alignment: .top), count: textSize.isAccessibilitySize ? 1 : 2), alignment: .leading, spacing: 20) {
                                ForEach(results) { card in
                                    NavigationLink {
                                        CardDetailsView(store: store, card: card, artworkDirectory: artworkDirectory)
                                    } label: {
                                        CardTile(card: card, artworkDirectory: artworkDirectory, status: isCollection ? copyCount(store.quantity(of: card.id)) : "On wishlist")
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityIdentifier("card-\(card.id)")
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, DSSpacing.xl)
            .padding(.bottom, DSSpacing.xl)
            .frame(maxWidth: 700, alignment: .leading)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle(title)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { findingCard = true } label: { Label("Find a card", systemImage: "plus") }
                    .disabled(store.loadError != nil)
            }
        }
        .sheet(isPresented: $findingCard) { CatalogView(store: store, artworkDirectory: artworkDirectory) }
    }

    private var emptyState: some View {
        VStack(spacing: DSSpacing.lg) {
            HStack(spacing: DSSpacing.md) {
                ForEach(Array(store.catalog.prefix(2))) { card in
                    CardArtwork(card: card, directory: artworkDirectory).frame(maxWidth: 104)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(DSSpacing.xl)
            .background(Color.accentColor.opacity(0.10), in: RoundedRectangle(cornerRadius: 24))
            .accessibilityHidden(true)
            DSStatusView(isCollection ? "Every collection starts with one card." : "Your wishlist is empty", description: isCollection ? "Find a card you own and add your first copy. Your personal collection grows from here." : "Find a card you’re looking for and add it to your wishlist.") {
                CollectionActionButton(isCollection ? "Find your first card" : "Find a card") { findingCard = true }
                    .tint(Color("PrimaryButton"))
                    .frame(maxWidth: .infinity)
            }
            .multilineTextAlignment(.center)
        }
        .padding(.top, DSSpacing.xxl)
    }
}

private struct SearchField: View {
    let title: String
    @Binding var query: String
    var body: some View {
        DSTextField(LocalizedStringKey(title), text: $query, capitalization: .never, autocorrectionDisabled: true, submitLabel: .search, onSubmit: dismissKeyboard)
            .padding(DSSpacing.md)
            .background(Color("Surface"), in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct CardArtwork: View {
    let card: Card
    let directory: URL
    var body: some View {
        Group {
            if let artwork = UIImage(contentsOfFile: directory.appendingPathComponent(card.image).path) {
                Image(uiImage: artwork).resizable().scaledToFit()
            } else {
                ContentUnavailableView("Artwork unavailable", systemImage: "photo")
                    .aspectRatio(0.716, contentMode: .fit)
            }
        }
        .accessibilityLabel("\(card.name) card artwork")
    }
}

private struct CardTile: View {
    let card: Card
    let artworkDirectory: URL
    let status: String
    var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            CardArtwork(card: card, directory: artworkDirectory).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: DSSpacing.xs) {
                Text(card.name).font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                Text(card.identity).font(.caption).foregroundStyle(.secondary)
                Text(status).font(.caption).foregroundStyle(Color.accentColor)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct CatalogView: View {
    let store: CollectionStore
    let artworkDirectory: URL
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: DSSpacing.lg) {
                    Text("Search the card catalog").foregroundStyle(.secondary)
                    SearchField(title: "Search catalog", query: $query)
                    Text("Scarlet & Violet—151").font(.title2.weight(.semibold))
                    Text("Seven-card example catalog · Match the printed card number").font(.caption).foregroundStyle(.secondary)
                    let results = store.cards(matching: query, scope: .catalog)
                    if results.isEmpty {
                        DSStatusView("No cards found", description: "Search by Pokémon, set name, or card number.")
                    } else {
                        LazyVStack(spacing: DSSpacing.md) {
                            ForEach(results) { card in
                                NavigationLink {
                                    CardDetailsView(store: store, card: card, artworkDirectory: artworkDirectory)
                                } label: {
                                    HStack(spacing: DSSpacing.lg) {
                                        CardArtwork(card: card, directory: artworkDirectory).frame(width: 64).accessibilityHidden(true)
                                        VStack(alignment: .leading, spacing: DSSpacing.xs) {
                                            Text(card.name).font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                                            Text(card.identity).font(.caption).foregroundStyle(.secondary)
                                            Text(store.quantity(of: card.id) > 0 ? "\(copyCount(store.quantity(of: card.id))) owned" : (store.isWishlisted(card.id) ? "On wishlist" : "Not collected yet")).font(.caption).foregroundStyle(Color.accentColor)
                                        }
                                        Spacer(minLength: 0)
                                        Image(systemName: "chevron.right").foregroundStyle(.secondary).accessibilityHidden(true)
                                    }
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                                .accessibilityIdentifier("card-\(card.id)")
                                Divider()
                            }
                        }
                    }
                }
                .padding(.horizontal, DSSpacing.xl)
                .padding(.bottom, DSSpacing.xl)
                .frame(maxWidth: 700, alignment: .leading)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle("Find a card")
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() } } }
        }
    }
}

private struct CardDetailsView: View {
    let store: CollectionStore
    let card: Card
    let artworkDirectory: URL
    @State private var addingCopy = false
    @State private var addedConfirmation = false
    @State private var pendingDelete: CopyEntry?
    @State private var operationError: String?
    @State private var retryOperation: (() throws -> Void)?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.lg) {
                CardArtwork(card: card, directory: artworkDirectory)
                    .frame(maxWidth: 200)
                    .padding(DSSpacing.lg)
                    .frame(maxWidth: .infinity)
                    .background(Color("Surface"), in: RoundedRectangle(cornerRadius: 24))
                VStack(alignment: .leading, spacing: DSSpacing.xs) {
                    Text(card.name).font(.title2.weight(.semibold))
                    Text("Scarlet & Violet—\(card.identity)").font(.caption).foregroundStyle(.secondary)
                }
                ViewThatFits(in: .horizontal) {
                    HStack { badges }
                    VStack(alignment: .leading) { badges }
                }
                VStack(alignment: .leading, spacing: DSSpacing.md) {
                    HStack {
                        Text("Your copies").font(.headline)
                        Spacer()
                        Text("\(store.quantity(of: card.id))").font(.title2.weight(.semibold)).foregroundStyle(Color.accentColor)
                    }
                    if store.copies(of: card.id).isEmpty {
                        Text("No copies yet").foregroundStyle(.secondary)
                    } else {
                        ForEach(store.copies(of: card.id)) { entry in
                            VStack(alignment: .leading, spacing: DSSpacing.sm) {
                                Text("\(copyCount(entry.quantity)) · \(entry.finish) · \(entry.condition.rawValue)")
                                    .font(.subheadline)
                                if !entry.note.isEmpty { Text(entry.note).font(.footnote).foregroundStyle(.secondary) }
                                Text(entry.addedAt, style: .date).font(.caption).foregroundStyle(.secondary)
                                Button("Remove entry", role: .destructive) { pendingDelete = entry }
                                    .frame(minHeight: 44)
                                    .accessibilityHint("Remove this entry of \(copyCount(entry.quantity)) after confirmation")
                            }
                            if entry.id != store.copies(of: card.id).last?.id { Divider() }
                        }
                    }
                }
                .padding(DSSpacing.lg)
                .background(Color("Surface"), in: RoundedRectangle(cornerRadius: 16))
                if let error = operationError {
                    DSStatusView("Could not save", description: error, kind: .error) {
                        CollectionActionButton("Retry") { if let retryOperation { perform(retryOperation) } }.tint(Color("PrimaryButton"))
                    }
                }
                VStack(spacing: DSSpacing.xs) {
                    CollectionActionButton("Add a copy") { addingCopy = true }.tint(Color("PrimaryButton")).frame(maxWidth: .infinity)
                    CollectionActionButton(store.isWishlisted(card.id) ? "Remove from wishlist" : "Add to wishlist", intent: .secondary) {
                        let included = !store.isWishlisted(card.id)
                        perform { try store.setWishlist(cardID: card.id, included: included) }
                    }.frame(maxWidth: .infinity)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(DSSpacing.xl)
            .frame(maxWidth: 700, alignment: .leading)
            .frame(maxWidth: .infinity)
        }
        .navigationTitle("Card details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    let included = !store.isWishlisted(card.id)
                    perform { try store.setWishlist(cardID: card.id, included: included) }
                } label: { Image(systemName: store.isWishlisted(card.id) ? "heart.fill" : "heart") }
                .accessibilityLabel(store.isWishlisted(card.id) ? "Unsave wishlist card" : "Save wishlist card")
            }
        }
        .sheet(isPresented: $addingCopy, onDismiss: {}) {
            AddCopyView(store: store, card: card, artworkDirectory: artworkDirectory) { addedConfirmation = true }
        }
        .alert("Added to collection", isPresented: $addedConfirmation) {
            Button("Done", role: .cancel) {}
        } message: { Text("You now own \(copyCount(store.quantity(of: card.id))) of \(card.name).") }
        .alert("Remove this copy entry?", isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } })) {
            Button("Cancel", role: .cancel) { pendingDelete = nil }
            Button("Remove", role: .destructive) {
                if let entry = pendingDelete { perform { try store.delete(entryID: entry.id) } }
                pendingDelete = nil
            }
        } message: { Text("This removes \(copyCount(pendingDelete?.quantity ?? 0)). Your wishlist stays unchanged.") }
    }
    @ViewBuilder private var badges: some View {
        Text(store.quantity(of: card.id) > 0 ? "In your collection" : "Not collected yet")
            .font(.caption).padding(.horizontal, DSSpacing.md).padding(.vertical, DSSpacing.xs)
            .background(Color.accentColor.opacity(0.10), in: Capsule())
        Text(card.rarity).font(.caption).padding(.horizontal, DSSpacing.md).padding(.vertical, DSSpacing.xs)
            .background(Color("Surface"), in: Capsule())
    }
    private func perform(_ operation: @escaping () throws -> Void) {
        do {
            try operation()
            operationError = nil
            retryOperation = nil
        } catch {
            operationError = error.localizedDescription
            retryOperation = operation
        }
    }
}

private struct AddCopyView: View {
    let store: CollectionStore
    let card: Card
    let artworkDirectory: URL
    let onSaved: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var quantity = 1
    @State private var finish: String
    @State private var condition = CardCondition.nearMint
    @State private var note = ""
    @State private var saveError: String?

    init(store: CollectionStore, card: Card, artworkDirectory: URL, onSaved: @escaping () -> Void) {
        self.store = store
        self.card = card
        self.artworkDirectory = artworkDirectory
        self.onSaved = onSaved
        _finish = State(initialValue: card.finishes.first ?? "")
    }
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack(spacing: DSSpacing.lg) {
                        CardArtwork(card: card, directory: artworkDirectory).frame(width: 64).accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: DSSpacing.xs) {
                            Text(card.name).font(.title2.weight(.semibold))
                            Text(card.identity).font(.caption).foregroundStyle(.secondary)
                            Text("\(copyCount(store.quantity(of: card.id))) already owned").font(.caption).foregroundStyle(Color.accentColor)
                        }
                    }
                }
                Section {
                    Stepper(value: $quantity, in: 1...99) { Text("Quantity: \(quantity)").monospacedDigit() }
                        .accessibilityIdentifier("Quantity")
                    Picker("Finish", selection: $finish) {
                        ForEach(card.finishes, id: \.self) { Text($0).tag($0) }
                    }.accessibilityIdentifier("finish-picker")
                    Picker("Condition", selection: $condition) {
                        ForEach(CardCondition.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                    }.accessibilityIdentifier("condition-picker")
                    DSTextField("Note (optional)", text: $note, supportingText: "\(note.count)/500 characters", errorText: note.count > 500 ? "Notes must be 500 characters or fewer." : nil, submitLabel: .done, onSubmit: dismissKeyboard)
                }
                Section {
                    if let error = saveError {
                        DSStatusView("Could not save", description: error, kind: .error) { CollectionActionButton("Retry", action: save).tint(Color("PrimaryButton")) }
                    }
                    Text("Your collection will have \(copyCount(store.quantity(of: card.id) + quantity)) of this card.")
                        .font(.footnote).foregroundStyle(.secondary)
                    CollectionActionButton("Add \(copyCount(quantity))", isEnabled: note.count <= 500, action: save)
                        .tint(Color("PrimaryButton"))
                        .accessibilityIdentifier("save-copy")
                        .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Add a copy")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
        }
    }
    private func save() {
        do {
            try store.add(cardID: card.id, quantity: quantity, finish: finish, condition: condition, note: note)
            dismiss()
            onSaved()
        } catch { saveError = error.localizedDescription }
    }
}

private extension Card {
    var identity: String { "\(set) · \(number)/\(printedTotal)" }
}
private func copyCount(_ count: Int) -> String { "\(count) \(count == 1 ? "copy" : "copies")" }
@MainActor private func dismissKeyboard() {
    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
}

#if DEBUG
@MainActor private final class PreviewStorage: CollectionStorage {
    func read() throws -> Data? { nil }
    func write(_ data: Data) throws {}
}
#Preview("Empty collection") {
    NavigationStack { SavedCardsView(store: CollectionStore(catalog: [], storage: PreviewStorage()), scope: .collection, artworkDirectory: URL(fileURLWithPath: "/")) }
}
#Preview("Wishlist · large dark text") {
    NavigationStack { SavedCardsView(store: CollectionStore(catalog: [], storage: PreviewStorage()), scope: .wishlist, artworkDirectory: URL(fileURLWithPath: "/")) }
        .preferredColorScheme(.dark).environment(\.dynamicTypeSize, .accessibility3)
}
#Preview("Compact actions") {
    VStack(spacing: DSSpacing.xs) {
        CollectionActionButton("Add a copy") {}
        CollectionActionButton("Add to wishlist", intent: .secondary) {}
        CollectionActionButton("Add a copy", isEnabled: false) {}
    }
    .padding(DSSpacing.xl)
}
#endif

/// Compact native actions are local to the example. The shared DSButton keeps
/// its template-sized 44-point label; these use 30 points plus system chrome
/// for a 44-point standard button, expanding naturally for Dynamic Type.
private struct CollectionActionButton: View {
    let title: LocalizedStringKey
    let intent: DSButtonIntent
    let isEnabled: Bool
    let action: () -> Void

    init(_ title: LocalizedStringKey, intent: DSButtonIntent = .primary, isEnabled: Bool = true, action: @escaping () -> Void) {
        self.title = title
        self.intent = intent
        self.isEnabled = isEnabled
        self.action = action
    }

    @ViewBuilder var body: some View {
        switch intent {
        case .primary: control.buttonStyle(.borderedProminent)
        case .secondary, .destructive: control.buttonStyle(.bordered)
        }
    }

    private var control: some View {
        Button(role: intent == .destructive ? .destructive : nil, action: action) {
            Text(title)
                .font(.body)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(minHeight: 30)
        }
        .controlSize(.regular)
        .disabled(!isEnabled)
        .frame(minHeight: 44)
    }
}
