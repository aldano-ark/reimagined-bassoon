import XCTest
@testable import NativeTemplate

final class AppConfigTests: XCTestCase {
    private var validInfo: [String: Any] {
        ["AppEnvironment": "stg",
         "CFBundleIdentifier": "com.example.nativetemplate.stg",
         "CFBundleDisplayName": "NativeTemplate Stg",
         "APIBaseURL": "https://stg-api.example.com"]
    }

    func testStagingConfiguration() throws {
        let config = try AppConfig.load(info: validInfo)
        XCTAssertEqual(config.environment, .stg)
        XCTAssertEqual(config.applicationID, "com.example.nativetemplate.stg")
        XCTAssertEqual(config.displayName, "NativeTemplate Stg")
        XCTAssertEqual(config.apiBaseURL.absoluteString, "https://stg-api.example.com")
    }

    func testMissingValuesDoNotFallBack() {
        for key in validInfo.keys {
            var info = validInfo
            info.removeValue(forKey: key)
            XCTAssertThrowsError(try AppConfig.load(info: info)) { error in
                XCTAssertEqual(error as? ConfigurationError, .missingValue(key))
            }
        }
    }

    func testInvalidEndpointsAreRejected() {
        for value in ["", " ", "/relative", "https://", "http://api.example.com",
                      "https://user:password@api.example.com", "https://api.example.com?q=1",
                      "https://api.example.com#fragment", "https://api.example.com/%zz",
                      " https://api.example.com", "https://api.example.com/a b"] {
            var info = validInfo
            info["APIBaseURL"] = value
            XCTAssertThrowsError(try AppConfig.load(info: info)) { error in
                XCTAssertEqual(error as? ConfigurationError, .invalidValue("APIBaseURL"))
            }
        }
    }
    func testInvalidEnvironmentIdentityAndName() {
        for (key, value) in [("AppEnvironment", "qa"), ("AppEnvironment", " stg "),
                             ("CFBundleIdentifier", " "), ("CFBundleDisplayName", " ")] {
            var info = validInfo
            info[key] = value
            XCTAssertThrowsError(try AppConfig.load(info: info)) { error in
                XCTAssertEqual(error as? ConfigurationError, .invalidValue(key))
            }
        }
    }

    func testNonStringValuesAreRejected() {
        for key in validInfo.keys {
            var info = validInfo
            info[key] = 42
            XCTAssertThrowsError(try AppConfig.load(info: info)) { error in
                XCTAssertEqual(error as? ConfigurationError, .missingValue(key))
            }
        }
    }

    func testPackagedEnvironmentMatchesIdentity() throws {
        let config = try AppConfig.load()
        let expectedIDs: [AppEnvironment: String] = [
            .dev: "com.example.nativetemplate.dev",
            .stg: "com.example.nativetemplate.stg",
            .prod: "com.example.nativetemplate",
        ]
        XCTAssertEqual(config.applicationID, expectedIDs[config.environment])
    }

}
