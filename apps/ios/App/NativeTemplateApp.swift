import DesignSystem
import SwiftUI

@main
struct NativeTemplateApp: App {
    private let configuration = Result { try AppConfig.load() }

    var body: some Scene {
        WindowGroup {
#if DEBUG
            if ProcessInfo.processInfo.arguments.contains("--design-system-tests") {
                DesignSystemTestHost()
            } else {
                configuredContent
            }
#else
            configuredContent
#endif
        }
    }

    @ViewBuilder
    private var configuredContent: some View {
        switch configuration {
        case .success(let config):
            ContentView(config: config,
                        request: ApiRequestFactory(baseURL: config.apiBaseURL).healthRequest())
        case .failure(let error):
            DSStatusView("Configuration error", description: error.localizedDescription, kind: .error)
                .padding(DSSpacing.lg)
        }
    }
}
