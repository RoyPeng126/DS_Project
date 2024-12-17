package com.example.searchengine.engine;

import com.example.searchengine.model.Keyword;
import com.example.searchengine.model.Page;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class KeywordCounterEngine {

    public KeywordCounterEngine() {
        // 如果需要建構子注入其他資源或設定，可在此增加參數與邏輯
    }

    /**
     * 根據網頁HTML內容與關鍵字列表(含權重)計算該頁面(包含其子頁結構)的分數，
     * 並回傳一個包含此頁及其子頁的 Page 結構。
     * 
     * @param htmlContent 網頁的HTML內容(起始點)
     * @param keywords    已解析出的關鍵字及其權重列表
     * @param title       該頁面的標題 (由外部呼叫時提供)
     * @param url         該頁面的URL (由外部呼叫時提供)
     * @return Page物件 (包含自身及子頁面樹狀結構)
     */
    public Page getPageStructure(String htmlContent, List<Keyword> keywords, String title, String url) {
        // 第一步：取得關鍵字出現次數(包含子頁面)
        // TODO: 實作 analyzeOccurrences，以取得所有子頁結構與關鍵字出現次數
        Map<Keyword, Integer> keywordOccurrences = analyzeOccurrences(htmlContent, keywords);

        // 第二步：計算出現次數 * weight 的加總分數
        int score = calculateScore(keywordOccurrences);

        // 建立本頁面的 Page 物件
        Page currentPage = new Page(title, url, score);

        // TODO: analyzeOccurrences 時若取得子頁面的資訊，可在此將子頁面加入 currentPage
        // 範例（僅示意）： currentPage.addChild(...)

        return currentPage;
    }

    /**
     * 分析該頁面及其子頁面中所有 keyword 的出現次數。
     * 在此可以實作爬取子頁、遞迴分析的邏輯，最後回傳整個階層中各 keyword 的出現次數。
     */
    private Map<Keyword, Integer> analyzeOccurrences(String htmlContent, List<Keyword> keywords) {
        // TODO: 實作
        // 下方為範例空回傳
        Map<Keyword, Integer> occurrences = new HashMap<>();
        for (Keyword kw : keywords) {
            occurrences.put(kw, 0); // 預設0出現次數
        }

        // TODO: 在此實作子頁抓取邏輯，取得子頁HTML後再次呼叫 analyzeOccurrences 遞迴計算
        // 並將子頁面Occurrences合併回 occurrences

        return occurrences;
    }

    /**
     * 計算分數：將各Keyword的出現次數 * weight 後加總。
     */
    private int calculateScore(Map<Keyword, Integer> keywordOccurrences) {
        int totalScore = 0;
        for (Map.Entry<Keyword, Integer> entry : keywordOccurrences.entrySet()) {
            Keyword kw = entry.getKey();
            int occurrence = entry.getValue();
            double weight = kw.getWeight();
            totalScore += (int) (occurrence * weight);
        }
        return totalScore;
    }
}
