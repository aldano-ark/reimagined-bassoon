import SwiftUI

public enum DSStatusKind: Sendable, Equatable {
    case neutral, error
}

/// A message and optional composed action, with no retry or navigation policy of its own.
public struct DSStatusView<Action: View>: View {
    private let title: LocalizedStringKey
    private let description: String?
    private let kind: DSStatusKind
    private let action: Action

    public init(
        _ title: LocalizedStringKey,
        description: String? = nil,
        kind: DSStatusKind = .neutral,
        @ViewBuilder action: () -> Action
    ) {
        self.title = title
        self.description = description
        self.kind = kind
        self.action = action()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            Text(title)
                .font(.title3).bold()
                .foregroundStyle(kind == .error ? Color(uiColor: .systemRed) : Color.primary)
                .accessibilityAddTraits(.isHeader)
                .fixedSize(horizontal: false, vertical: true)
            if let description {
                Text(description)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            action
        }
        .padding(DSSpacing.lg)
    }
}

public extension DSStatusView where Action == EmptyView {
    init(_ title: LocalizedStringKey, description: String? = nil, kind: DSStatusKind = .neutral) {
        self.init(title, description: description, kind: kind) { EmptyView() }
    }
}
