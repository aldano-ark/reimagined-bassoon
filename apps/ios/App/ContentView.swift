import DesignSystem
import SwiftUI

struct ContentView: View {
    var body: some View {
        Text("NativeTemplate")
            .padding(DSSpacing.lg)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(uiColor: .systemBackground))
    }
}
