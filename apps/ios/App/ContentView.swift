import DesignSystem
import Foundation
import SwiftUI

struct ContentView: View {
    let config: AppConfig
    let request: URLRequest

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.lg) {
                Text(config.displayName)
                    .font(.title2)
                    .accessibilityAddTraits(.isHeader)
                Text("Environment: \(config.environment.rawValue)")
                Text("API base URL: \(config.apiBaseURL.absoluteString)")
                Text("\(request.httpMethod ?? "GET") \(request.url?.absoluteString ?? "")")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DSSpacing.lg)
        }
        .background(Color(uiColor: .systemBackground))
    }
}
