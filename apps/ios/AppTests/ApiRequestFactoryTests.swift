import XCTest
@testable import NativeTemplate

final class ApiRequestFactoryTests: XCTestCase {
    func testHealthRequestPreservesBasePathAndEncoding() throws {
        let cases = [
            ("https://stg-api.example.com", "https://stg-api.example.com/health"),
            ("https://stg-api.example.com/", "https://stg-api.example.com/health"),
            ("https://api.example.com/v1", "https://api.example.com/v1/health"),
            ("https://api.example.com/v1/", "https://api.example.com/v1/health"),
            ("https://api.example.com:8443/a%2Fb/", "https://api.example.com:8443/a%2Fb/health"),
        ]
        for (raw, expected) in cases {
            let base = try XCTUnwrap(URL(string: raw))
            let request = ApiRequestFactory(baseURL: base).healthRequest()
            XCTAssertEqual(request.httpMethod, "GET")
            XCTAssertEqual(request.url?.absoluteString, expected)
            XCTAssertNil(request.httpBody)
        }
    }
}
