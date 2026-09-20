#if DEBUG
import DesignSystem
import SwiftUI

/// Test-only fixtures selected by launch arguments; ordinary app launches never enter this view.
struct DesignSystemTestHost: View {
    @State private var count = 0
    @State private var loading = true
    @State private var value = ""
    @State private var error: String?

    private var largeRTL: Bool {
        ProcessInfo.processInfo.arguments.contains("--ds-large-rtl")
    }

    private var scenario: String {
        ProcessInfo.processInfo.arguments
            .first { $0.hasPrefix("--ds-scenario=") }?
            .replacingOccurrences(of: "--ds-scenario=", with: "") ?? "buttons"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.lg) {
                switch scenario {
                case "field":
                    DSTextField("Name", text: $value, supportingText: "Helpful text", errorText: error,
                                capitalization: .never, autocorrectionDisabled: true)
                    Button("Set externally") { value = "Grace" }
                    Button("Show error") { error = "Required" }
                    Text("Value: \(value)").accessibilityIdentifier("field-value")
                case "status":
                    DSStatusView("Could not load", description: "Try again", kind: .error) {
                        DSButton("Retry") { count += 1 }
                    }
                default:
                    let title: LocalizedStringKey = largeRTL
                        ? "A longer action label that needs to wrap"
                        : "Save"
                    DSButton(title, isLoading: loading) { count += 1 }
                        .accessibilityIdentifier("save-action")
                    Button("Finish loading") { loading = false }
                    if !largeRTL {
                        DSButton("Disabled", isEnabled: false) { count += 1 }
                        DSButton("Inherited disabled") { count += 1 }.disabled(true)
                        DSButton("Secondary", intent: .secondary) { count += 1 }
                        DSButton("Delete", intent: .destructive) { count += 1 }
                    }
                }
                Text("Count: \(count)").accessibilityIdentifier("activation-count")
            }
            .padding(DSSpacing.lg)
        }
        .dynamicTypeSize(largeRTL ? .accessibility3 : .large)
        .environment(\.layoutDirection, largeRTL ? .rightToLeft : .leftToRight)
    }
}
#endif
