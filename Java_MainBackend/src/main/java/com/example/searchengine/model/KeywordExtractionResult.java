package com.example.searchengine.model;

import java.util.List;

public class KeywordExtractionResult {
    private String combinedKeywords; // 用空白分隔的一串關鍵字
    private List<Keyword> keywordList; // 每個Keyword物件有word與weight

    public KeywordExtractionResult(String combinedKeywords, List<Keyword> keywordList) {
        this.combinedKeywords = combinedKeywords;
        this.keywordList = keywordList;
    }

    public String getCombinedKeywords() {
        return combinedKeywords;
    }

    public List<Keyword> getKeywordList() {
        return keywordList;
    }
}
