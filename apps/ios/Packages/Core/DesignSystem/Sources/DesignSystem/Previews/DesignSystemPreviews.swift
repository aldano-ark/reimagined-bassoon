#if DEBUG
import SwiftUI

private struct DesignSystemGallery: View {
    @State private var value = "Example"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                Text("Buttons").font(.headline)
                DSButton("Primary action") {}
                DSButton("Secondary action", intent: .secondary) {}
                DSButton("Destructive action", intent: .destructive) {}
                DSButton("Disabled action", isEnabled: false) {}
                DSButton("Loading action", isLoading: true) {}
                Text("Fields").font(.headline)
                DSTextField("Label", text: $value, supportingText: "Supporting text")
                DSTextField("Required field", text: .constant(""), errorText: "Enter a value")
                DSTextField("Disabled field", text: .constant("Unavailable"), isEnabled: false)
                DSStatusView("No content", description: "Nothing to display yet")
                DSStatusView("Could not load", description: "Try again", kind: .error) {
                    DSButton("Retry", intent: .secondary) {}
                }
            }
            .padding(DSSpacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color(uiColor: .systemBackground))
    }
}

#Preview("Light") { DesignSystemGallery().preferredColorScheme(.light) }
#Preview("Dark") { DesignSystemGallery().preferredColorScheme(.dark) }
#Preview("Large text") { DesignSystemGallery().dynamicTypeSize(.accessibility3) }
#Preview("RTL") { DesignSystemGallery().environment(\.layoutDirection, .rightToLeft) }
#endif
