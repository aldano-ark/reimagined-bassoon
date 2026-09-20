import Foundation

enum AppEnvironment: String, CaseIterable { case dev, stg, prod }

enum ConfigurationError: LocalizedError, Equatable {
    case missingValue(String)
    case invalidValue(String)

    var errorDescription: String? {
        switch self {
        case .missingValue(let key):
            return "Missing configuration value: \(key). Update configuration and rebuild."
        case .invalidValue(let key):
            return "Invalid configuration value: \(key). Update configuration and rebuild."
        }
    }
}

struct AppConfig {
    let environment: AppEnvironment
    let applicationID: String
    let displayName: String
    let apiBaseURL: URL

    private init(environment: AppEnvironment, applicationID: String,
                 displayName: String, apiBaseURL: URL) {
        self.environment = environment
        self.applicationID = applicationID
        self.displayName = displayName
        self.apiBaseURL = apiBaseURL
    }

    static func load(bundle: Bundle = .main) throws -> AppConfig {
        try load(info: bundle.infoDictionary ?? [:])
    }

    static func load(info: [String: Any]) throws -> AppConfig {
        func required(_ key: String) throws -> String {
            guard let value = info[key] as? String else {
                throw ConfigurationError.missingValue(key)
            }
            guard !value.isEmpty,
                  value == value.trimmingCharacters(in: .whitespacesAndNewlines),
                  value.rangeOfCharacter(from: .controlCharacters) == nil else {
                throw ConfigurationError.invalidValue(key)
            }
            return value
        }
        let rawEnvironment = try required("AppEnvironment")
        guard let environment = AppEnvironment(rawValue: rawEnvironment) else {
            throw ConfigurationError.invalidValue("AppEnvironment")
        }
        let applicationID = try required("CFBundleIdentifier")
        let displayName = try required("CFBundleDisplayName")
        let rawURL = try required("APIBaseURL")
        guard rawURL.rangeOfCharacter(from: .whitespacesAndNewlines) == nil,
              let url = URL(string: rawURL, encodingInvalidCharacters: false),
              let parts = URLComponents(url: url, resolvingAgainstBaseURL: false),
              parts.scheme?.lowercased() == "https", let host = parts.host, !host.isEmpty,
              parts.user == nil, parts.password == nil,
              parts.query == nil, parts.fragment == nil else {
            throw ConfigurationError.invalidValue("APIBaseURL")
        }
        return AppConfig(environment: environment, applicationID: applicationID,
                         displayName: displayName, apiBaseURL: url)
    }
}
