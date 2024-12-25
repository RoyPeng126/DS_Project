import Foundation

struct SearchResult: Identifiable, Decodable {
    // SwiftUI 顯示需要的唯一識別符，不從後端來
    let id = UUID()
    
    let title: String
    let url: String
    let aggregatedScore: Int
    let snippet: String
    let scoreDetails: [String: String]?
    
    // 告訴 Swift 解碼器：只有以下 key 要從 JSON 解
    private enum CodingKeys: String, CodingKey {
        case title
        case url
        case aggregatedScore
        case snippet
        case scoreDetails
    }
}

