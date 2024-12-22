import Foundation

struct SearchResultPage {
    let title: String
    let url: String
}

class GoogleQueryService {
    func performSearch(with query: String, completion: @escaping (Result<[SearchResultPage], Error>) -> Void) {
        DispatchQueue.global().async {
            // 模擬網路延遲
            sleep(2)

            // 檢查查詢是否為空
            if query.trimmingCharacters(in: .whitespaces).isEmpty {
                DispatchQueue.main.async {
                    let error = NSError(domain: "InvalidQuery", code: 400, userInfo: [NSLocalizedDescriptionKey: "Query is empty"])
                    completion(.failure(error))
                }
                return
            }

            // 模擬搜尋結果
            let mockPages = [
                SearchResultPage(title: "Example Page 1", url: "https://example.com/1"),
                SearchResultPage(title: "Example Page 2", url: "https://example.com/2")
            ]

            DispatchQueue.main.async {
                        completion(.success(mockPages))
            }
        }
    }
}
