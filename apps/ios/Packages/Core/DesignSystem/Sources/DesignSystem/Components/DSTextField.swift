import SwiftUI
import UIKit

/// A native field that forwards editing to its binding and displays caller-supplied validation.
public struct DSTextField: View {
    private let label: LocalizedStringKey
    @Binding private var text: String
    @FocusState private var isFocused: Bool
    @Environment(\.isEnabled) private var environmentEnabled
    private let isEnabled: Bool
    private let supportingText: String?
    private let errorText: String?
    private let keyboardType: UIKeyboardType
    private let contentType: UITextContentType?
    private let capitalization: TextInputAutocapitalization?
    private let autocorrectionDisabled: Bool
    private let submitLabel: SubmitLabel
    private let onSubmit: () -> Void

    public init(
        _ label: LocalizedStringKey,
        text: Binding<String>,
        isEnabled: Bool = true,
        supportingText: String? = nil,
        errorText: String? = nil,
        keyboardType: UIKeyboardType = .default,
        contentType: UITextContentType? = nil,
        capitalization: TextInputAutocapitalization? = .sentences,
        autocorrectionDisabled: Bool = false,
        submitLabel: SubmitLabel = .return,
        onSubmit: @escaping () -> Void = {}
    ) {
        self.label = label
        self._text = text
        self.isEnabled = isEnabled
        self.supportingText = supportingText
        self.errorText = errorText
        self.keyboardType = keyboardType
        self.contentType = contentType
        self.capitalization = capitalization
        self.autocorrectionDisabled = autocorrectionDisabled
        self.submitLabel = submitLabel
        self.onSubmit = onSubmit
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.xs) {
            Text(label).font(.subheadline).accessibilityHidden(true)
            TextField("", text: $text)
                .textFieldStyle(.roundedBorder)
                .focused($isFocused)
                .accessibilityLabel(Text(label))
                .keyboardType(keyboardType)
                .textContentType(contentType)
                .textInputAutocapitalization(capitalization)
                .autocorrectionDisabled(autocorrectionDisabled)
                .submitLabel(submitLabel)
                .onSubmit(onSubmit)
                .disabled(!isEnabled)
                .frame(minHeight: 44)
                .contentShape(Rectangle())
                // The native editing rect can be smaller than the minimum touch target.
                // A simultaneous gesture preserves native editing/selection gestures.
                .simultaneousGesture(TapGesture().onEnded {
                    if isEnabled && environmentEnabled { isFocused = true }
                })
            if let message = errorText ?? supportingText {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(errorText == nil ? Color.secondary : Color(uiColor: .systemRed))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}
