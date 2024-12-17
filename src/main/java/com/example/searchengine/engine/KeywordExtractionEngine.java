package com.example.searchengine.engine;

import com.example.searchengine.model.KeywordExtractionResult;

public interface KeywordExtractionEngine {
    /**
     * 將使用者原始輸入字串進行分析：
     * 1. 抽取關鍵字並計算權重
     * 2. 回傳一個 KeywordExtractionResult，其中包含：
     *    - 已組合的關鍵字字串(空白分隔)
     *    - 關鍵字與權重的列表
     */
    KeywordExtractionResult extractKeywords(String userInput);
}
