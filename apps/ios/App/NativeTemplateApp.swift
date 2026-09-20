import SwiftUI

@main
struct NativeTemplateApp: App {
    var body: some Scene {
        WindowGroup {
#if DEBUG
            if ProcessInfo.processInfo.arguments.contains("--design-system-tests") {
                DesignSystemTestHost()
            } else {
                ContentView()
            }
#else
            ContentView()
#endif
        }
    }
}
