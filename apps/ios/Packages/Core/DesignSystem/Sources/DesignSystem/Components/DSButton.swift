import SwiftUI

public enum DSButtonIntent: Sendable, Equatable {
    case primary, secondary, destructive
}

/// A native action whose execution and loading state are owned by its caller.
public struct DSButton: View {
    private let title: LocalizedStringKey
    private let intent: DSButtonIntent
    private let isEnabled: Bool
    private let isLoading: Bool
    private let action: () -> Void

    public init(
        _ title: LocalizedStringKey,
        intent: DSButtonIntent = .primary,
        isEnabled: Bool = true,
        isLoading: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.intent = intent
        self.isEnabled = isEnabled
        self.isLoading = isLoading
        self.action = action
    }

    @ViewBuilder public var body: some View {
        switch intent {
        case .primary:
            control.buttonStyle(.borderedProminent)
        case .secondary, .destructive:
            control.buttonStyle(.bordered)
        }
    }

    private var control: some View {
        Button(role: intent == .destructive ? .destructive : nil, action: action) {
            HStack(spacing: DSSpacing.sm) {
                if isLoading {
                    ProgressView().controlSize(.small).accessibilityHidden(true)
                }
                Text(title)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(minHeight: 44)
        }
        .disabled(!isEnabled || isLoading)
        .accessibilityLabel(Text(title))
        .accessibilityValue(isLoading ? Text("design_system_loading", bundle: .module) : Text(""))
    }
}
