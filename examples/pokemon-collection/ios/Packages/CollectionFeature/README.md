# CollectionFeature

An iOS 17+ Swift package that depends on the read-only native DesignSystem. `SavedCardsView` is the public saved-card destination; the app supplies its `CollectionStore`, `.collection` or `.wishlist` scope, artwork directory, and root navigation.

`CardCatalog.load` decodes the bundled catalog. `CollectionStore` is observable and isolated to the main actor. Its injected `CollectionStorage` serializes small local documents on that same actor; `FileCollectionStorage` creates the parent directory and replaces data atomically. The app owns the storage URL. No package code reads app launch arguments or depends on app entry points.

A mutation builds and validates a candidate snapshot, writes it, then publishes it. Reload validates schema version, card references, per-entry IDs, wishlist uniqueness, supported finishes, quantities, conditions, notes, and timestamps before replacing live state. Failed loads retain the last state and block writes. Wishlist membership is idempotent and independent of ownership.

The Xcode `CollectionTests` target exercises this package with real temporary file storage and injected read/write failures. `CollectionUITests` exercises the assembled app and captures screenshot evidence. Source previews compile only in Debug.
