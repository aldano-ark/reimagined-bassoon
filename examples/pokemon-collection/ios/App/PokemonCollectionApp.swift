import SwiftUI
import CollectionFeature

@main
struct PokemonCollectionApp: App {
    @State private var store: CollectionStore?
    @State private var startupError: String?
    private let artworkDirectory = Bundle.main.resourceURL!.appendingPathComponent("assets")

    var body: some Scene {
        WindowGroup {
            Group {
                if let store {
                    TabView {
                        NavigationStack {
                            SavedCardsView(store: store, scope: .collection, artworkDirectory: artworkDirectory)
                        }
                        .tabItem { Label("Collection", systemImage: "book") }
                        NavigationStack {
                            SavedCardsView(store: store, scope: .wishlist, artworkDirectory: artworkDirectory)
                        }
                        .tabItem { Label("Wishlist", systemImage: "heart") }
                    }
                } else if let startupError {
                    ContentUnavailableView {
                        Label("Catalog unavailable", systemImage: "exclamationmark.triangle")
                    } description: { Text(startupError) } actions: { Button("Retry", action: assemble) }
                } else { ProgressView("Loading collection") }
            }
            .task { if store == nil { assemble() } }
            .preferredColorScheme(testAppearance)
        }
    }

    private var testAppearance: ColorScheme? {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("--ui-test-dark") ? .dark : nil
        #else
        nil
        #endif
    }

    private func assemble() {
        do {
            let catalog = try CardCatalog.load(from: artworkDirectory.appendingPathComponent("catalog.json"))
            var filename = "collection.json"
            #if DEBUG
            // Each UI test owns a separate file; relaunch reuses it. Release ignores test flags.
            let arguments = ProcessInfo.processInfo.arguments
            if let index = arguments.firstIndex(of: "--ui-test-storage"), arguments.indices.contains(index + 1), let id = UUID(uuidString: arguments[index + 1]) {
                filename = "ui-test-\(id.uuidString).json"
            }
            #endif
            let directory = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
                .appendingPathComponent("PokemonCollection", isDirectory: true)
            store = CollectionStore(catalog: catalog, storage: FileCollectionStorage(url: directory.appendingPathComponent(filename)))
            startupError = nil
        } catch { startupError = error.localizedDescription }
    }
}
