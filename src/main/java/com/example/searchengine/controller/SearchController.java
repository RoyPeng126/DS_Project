package com.example.searchengine.controller;

import com.example.searchengine.engine.KeywordExtractionEngine;
import com.example.searchengine.engine.KeywordCounterEngine;
import com.example.searchengine.model.Keyword;
import com.example.searchengine.model.KeywordExtractionResult;
import com.example.searchengine.model.Page;
import com.example.searchengine.service.GoogleQuery;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Controller
public class SearchController {

    private final KeywordExtractionEngine keywordExtractionEngine;
    private final KeywordCounterEngine keywordCounterEngine;

    public SearchController(KeywordExtractionEngine keywordExtractionEngine, KeywordCounterEngine keywordCounterEngine) {
        this.keywordExtractionEngine = keywordExtractionEngine;
        this.keywordCounterEngine = keywordCounterEngine;
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
        try {
            // 第一步：抽取關鍵字與權重
            // TODO: 待完成 keywordExtractionEngine.java, KeywordExtractionResult.java
            // 先利用 Google 翻譯 API 把使用者輸入轉成繁體中文
            // 會先把自然語言 (若有) 利用 CKIP 切成 Keywords 形式 (空格分開) -> 要設定條件才觸發，在使用者已經用關鍵字形式輸入時就不觸發
            // 利用分類模型 (準確率無所謂，只影響 Weight) 為每個關鍵字分類到 “城市“、”夜市名“、”食物類型“、”食物名“ 任一個，給上 weights 返回 List & String
            // 對於被分到 “城市“、”夜市名“ 這兩類的關鍵字，要另用 2 個表來比對相似度，取出表中相似度 (Voyage Re-ranker) 最高的，替換掉原來的關鍵字
            // -> 這步要驗證清楚準確率，或是對於中文輸入就跳過這步，不然 DEMO 時匹配到另一個夜市，就徹底出包了
            // -> 或是保留原本關鍵字和 Mapping 後的一起 Google Search？
            // 再把關鍵字做 Expand (利用同義詞方式為關鍵字增廣，增加搜尋廣度) (Optional, 真的有大幅幫助再做)
            // -> 這步只在 keywordList 儲存，不放入 combinedKeywords 在 google search 使用，怕影響語意搜尋成效
            // -> Expand Method: https://blog.csdn.net/stay_foolish12/article/details/113108919
            KeywordExtractionResult extractionResult = keywordExtractionEngine.extractKeywords(query);
            String combinedKeywords = extractionResult.getCombinedKeywords();
            List<Keyword> keywordList = extractionResult.getKeywordList();

            // 第二步：Google搜尋，取得前 50 筆結果 (title->url)
            // 已完成
            // 可能 TODO: 如果發現 Google Search 真的不好打敗，尋找其他適合查詢夜市美食的搜尋引擎，直接替換 Google 結果
            GoogleQuery googleQuery = new GoogleQuery(combinedKeywords);
            Map<String, String> initialResults = googleQuery.query();

            // 第三步：對每個 result 取得 Page 結構並計算樹狀分數
            // TODO: 待完成 keywordCounterEngine.java
            List<RootPageResult> rootPageResults = new ArrayList<>();
            for (Map.Entry<String, String> entry : initialResults.entrySet()) {
                String title = entry.getKey();
                String pageUrl = entry.getValue();

                // 抓取網頁HTML
                String htmlContent = fetchHtmlContent(pageUrl);
                
                int depth = 2; // 設定遞迴深度 (暫定)

                // 取得此頁(及其子頁)的 Page 結構
                Page rootPage = keywordCounterEngine.getPageStructure(htmlContent, keywordList, title, pageUrl, depth);

                // 計算整棵樹的總分數(包括子頁、子子頁...)
                int aggregatedScore = computeAggregatedScore(rootPage);

                rootPageResults.add(new RootPageResult(rootPage.getTitle(), rootPage.getUrl(), aggregatedScore));
            }

            // 第四步：依最終分數排序(高->低)
            rootPageResults.sort((r1, r2) -> Integer.compare(r2.getAggregatedScore(), r1.getAggregatedScore()));

            // 第五步：二次排序 (Optional)
            // 先跑過測試集，如果準確率表現極為不佳可加入
            // 1. embedding vector search
            // 2. bm25f search
            // 3. voyage (或之類其他廠家的) reranker
            // 4. knowledge graph

            // 傳給前端
            // TODO: 還需要回傳 Top 文字雲 Keywords (TF-IDF, 抓 Google 提供的, etc.)
            Map<String, String> sortedResults = new LinkedHashMap<>();
            for (RootPageResult rpr : rootPageResults) {
                sortedResults.put(rpr.getTitle(), rpr.getUrl());
            }
            model.addAttribute("results", sortedResults);
            model.addAttribute("query", query);

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Error fetching results");
        }
        return "index";
    }

    /**
     * 抓取網頁HTML內容
     */
    private String fetchHtmlContent(String pageUrl) throws IOException {
        StringBuilder sb = new StringBuilder();
        URL u = new URL(pageUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("User-Agent", "Chrome/107.0.5304.107");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 計算某個 Page (含子頁) 的總分數：為該 Page 的 score 加上所有子孫頁面的 score。
     */
    private int computeAggregatedScore(Page root) {
        int total = root.getScore();
        for (Page child : root.getChildren()) {
            total += computeAggregatedScore(child);
        }
        return total;
    }

    /**
     * 用來儲存 root page 的結果(包含最終聚合分數)的內部類別
     */
    private static class RootPageResult {
        private final String title;
        private final String url;
        private final int aggregatedScore;

        public RootPageResult(String title, String url, int aggregatedScore) {
            this.title = title;
            this.url = url;
            this.aggregatedScore = aggregatedScore;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public int getAggregatedScore() {
            return aggregatedScore;
        }
    }
}
