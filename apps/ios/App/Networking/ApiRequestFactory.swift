import Foundation

struct ApiRequestFactory {
    let baseURL: URL

    func healthRequest() -> URLRequest {
        var request = URLRequest(url: baseURL.appendingPathComponent("health", isDirectory: false))
        request.httpMethod = "GET"
        return request
    }
}
