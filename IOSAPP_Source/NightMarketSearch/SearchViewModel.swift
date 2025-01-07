import Foundation
import Combine

class SearchViewModel: ObservableObject {
    // 用於綁定搜尋輸入和結果
    @Published var query: String = "" // 搜尋輸入
    @Published var results: [SearchResult] = [] // 搜尋結果
    @Published var resultTexts: [String] = [] // 相關關鍵字

    // 錯誤訊息或狀態顯示
    @Published var errorMessage: String? = nil

    private var cancellables = Set<AnyCancellable>() // 管理 Combine 訂閱

    // 搜尋功能
    func search() {
        guard !query.isEmpty else {
            errorMessage = "Query cannot be empty."
            return
        }

        // 清空先前的結果與錯誤
        results = []
        resultTexts = []
        errorMessage = nil

        // API URL
        guard let url = URL(string: "http://localhost:8080/api/search?query=\(query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") else {
            errorMessage = "Invalid API URL."
            return
        }

        // 發送請求
        URLSession.shared.dataTaskPublisher(for: url)
            .tryMap { data, response -> Data in
                guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                    throw URLError(.badServerResponse)
                }
                return data
            }
            .decode(type: SearchResponse.self, decoder: JSONDecoder())
            .receive(on: DispatchQueue.main)
            .sink(receiveCompletion: { [weak self] completion in
                switch completion {
                case .failure(let error):
                    self?.errorMessage = "Failed to load data: \(error.localizedDescription)"
                case .finished:
                    break
                }
            }, receiveValue: { [weak self] response in
                self?.results = response.results
                self?.resultTexts = response.resultTexts
            })
            .store(in: &cancellables)
    }
}

// 後端回傳的 JSON 結構
struct SearchResponse: Decodable {
    let resultTexts: [String]
    let results: [SearchResult]
}

