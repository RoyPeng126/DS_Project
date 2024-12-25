// SearchViewModel.swift
// TODO: Old Version, New one been delete.

import SwiftUI

class SearchViewModel: ObservableObject {
    @Published var query: String = ""          // 綁定 TextField
    @Published var results: [SearchResult] = [] // 搜尋結果列表
    @Published var errorMessage: String?        // 錯誤訊息
    @Published var relatedKeywords: [String] = [] // 類似 "Related Keywords"

    func search() {
        guard !query.isEmpty else { return }

        // 每次搜尋前先清空
        self.results = []
        self.errorMessage = nil
        self.relatedKeywords = []

        guard let url = URL(string: "http://localhost:8080/search?query=\(query)") else {
            self.errorMessage = "URL 錯誤"
            return
        }

        // 建立請求
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        // 發出網路請求
        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                guard let self = self else { return }
                
                if let error = error {
                    self.errorMessage = "發生錯誤: \(error.localizedDescription)"
                    return
                }
                
                // 狀態碼檢查 (200~299 代表成功)
                if let httpResponse = response as? HTTPURLResponse,
                   !(200...299).contains(httpResponse.statusCode) {
                    self.errorMessage = "Server 回傳錯誤狀態：\(httpResponse.statusCode)"
                    return
                }
                
                // 確定有 data
                guard let data = data else {
                    self.errorMessage = "沒有資料"
                    return
                }

                do {
                    // {
                    //   "results": [
                    //       { "title": "...", "snippet": "...", "url": "...", "aggregatedScore": 7.5 },
                    //       ...
                    //   ],
                    //   "relatedKeywords": ["關鍵字1", "關鍵字2", ...]
                    // }
                    struct APIResponse: Decodable {
                        let results: [SearchResult]?
                        let relatedKeywords: [String]?
                    }

                    let decoded = try JSONDecoder().decode(APIResponse.self, from: data)
                    
                    if let results = decoded.results {
                        self.results = results
                    }
                    if let related = decoded.relatedKeywords {
                        self.relatedKeywords = related
                    }

                } catch {
                    self.errorMessage = "JSON 解析失敗: \(error.localizedDescription)"
                }
            }
        }.resume()
    }
}
