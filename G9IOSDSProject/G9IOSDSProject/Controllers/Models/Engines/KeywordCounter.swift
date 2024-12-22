import Foundation

class KeywordCounter {
    func countKeywords(in text: String) -> [Keyword] {
        var keywordMap: [String: Int] = [:]
        let words = text.split(separator: " ").map { String($0) }
        for word in words {
            keywordMap[word, default: 0] += 1
        }
        return keywordMap.map { Keyword(term: $0.key, frequency: $0.value) }
    }
}