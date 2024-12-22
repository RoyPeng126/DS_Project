import Foundation

class KeywordExtraction {
    private let keywordCounter = KeywordCounter()

    func extractKeywords(from text: String) -> KeywordExtractionResult {
        let keywords = keywordCounter.countKeywords(in: text)
        return KeywordExtractionResult(keywords: keywords, totalKeywords: keywords.count)
    }
}