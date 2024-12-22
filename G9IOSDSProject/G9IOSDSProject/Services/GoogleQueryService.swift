import Foundation

class GoogleQueryService {
    func performSearch(with query: String, completion: @escaping ([Page]) -> Void) {
        // Mock implementation: Replace with actual network call
        DispatchQueue.global().async {
            let mockPages = [
                Page(title: "Example Page 1", url: "https://example.com/1", keywordExtractionResult: nil),
                Page(title: "Example Page 2", url: "https://example.com/2", keywordExtractionResult: nil)
            ]
            DispatchQueue.main.async {
                completion(mockPages)
            }
        }
    }
}
