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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
public class SearchController {

    private final KeywordExtractionEngine keywordExtractionEngine;
    private final KeywordCounterEngine keywordCounterEngine;

    private final Map<String, String> htmlCache = new ConcurrentHashMap<>();

    private boolean isDownloadLink(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".pdf") 
            || lower.endsWith(".zip")
            || lower.contains("download.ashx")
            || lower.contains("/download/");
    }

    public SearchController(KeywordExtractionEngine keywordExtractionEngine, KeywordCounterEngine keywordCounterEngine) {
        this.keywordExtractionEngine = keywordExtractionEngine;
        this.keywordCounterEngine = keywordCounterEngine;
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
        try {
            // 第一步：關鍵字處理 (與原本相同)
            KeywordExtractionResult extractionResult = keywordExtractionEngine.extractKeywords(query);
            List<Keyword> keywordList = extractionResult.getKeywordList();
            String combinedKeywords = extractionResult.getCombinedKeywords();
            String combinedKeywordsgoo = combinedKeywords + "夜市 美食";

            // 第二步：Google搜尋，取得前 50 筆結果
            GoogleQuery googleQuery = new GoogleQuery(combinedKeywordsgoo);
            Map<String, String> initialResults = googleQuery.query(); // title -> url

            // ===★ 新增：多執行緒平行抓取，並把 depth 設為 0★===
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<RootPageResult>> futures = new ArrayList<>();

            // 逐筆提交抓網頁的任務
            for (Map.Entry<String, String> entry : initialResults.entrySet()) {
                futures.add(executor.submit(() -> {
                    String title = entry.getKey();
                    String pageUrl = entry.getValue();

                    // (1) 過濾 PDF/下載 連結
                    if (isDownloadLink(pageUrl)) {
                        System.out.println("isDownloadLink");
                        return new RootPageResult(title, pageUrl, 0, new HashMap<>());
                    }

                    // (2) 抓網頁
                    String htmlContent = fetchHtmlContent(pageUrl); 
                    if (htmlContent.isEmpty()) {
                        // 403 或其它失敗 => 分數=0
                        System.out.println("htmlContent");
                        return new RootPageResult(title, pageUrl, 0, new HashMap<>());
                    }

                    // (3) depth=0，不再抓子頁
                    Page rootPage = keywordCounterEngine.getPageStructure(htmlContent, keywordList, title, pageUrl, 0);
                    int aggregatedScore = rootPage.getScore();
                    Map<String, String> scoreDetails = rootPage.getScoreDetails(); // 從 Page 取得分數細節
                    return new RootPageResult(title, pageUrl, aggregatedScore, scoreDetails);
                }));
            }

            // 等所有任務完成
            executor.shutdown();
            List<RootPageResult> rootPageResults = new ArrayList<>();
            for (Future<RootPageResult> f : futures) {
                RootPageResult rpr = f.get(); // block 直到該任務跑完
                rootPageResults.add(rpr);
            }

            // 第四步：依最終分數排序(高->低)
            rootPageResults.sort((r1, r2) -> Integer.compare(r2.getAggregatedScore(), r1.getAggregatedScore()));

            // 傳給前端
            Map<String, String> sortedResults = new LinkedHashMap<>();
            for (RootPageResult rpr : rootPageResults) {
                sortedResults.put(rpr.getTitle(), rpr.getUrl());
            }

            System.out.println("Sorted Results:");
            for (RootPageResult rpr : rootPageResults) {
                System.out.println("Title: " + rpr.getTitle() + ", URL: " + rpr.getUrl() + ", Score: " + rpr.getAggregatedScore());
                System.out.println("Score Details:");
                for (Map.Entry<String, String> entry : rpr.getScoreDetails().entrySet()) {
                    System.out.println("    Keyword: " + entry.getKey() + ", Calculation: " + entry.getValue());
                }
                System.out.println();
            }

            model.addAttribute("results", sortedResults);
            model.addAttribute("query", combinedKeywords);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error fetching results");
        }
        return "index";
    }

    /**
     * 抓取網頁HTML內容，加入 User-Agent / Referer / 403 處理 + Cache
     */
    private String fetchHtmlContent(String pageUrl) throws IOException {
        // 1. 檢查快取
        if (htmlCache.containsKey(pageUrl)) {
            return htmlCache.get(pageUrl); // 直接回傳已抓好的HTML
        }

        // 2. 協定檢查
        if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://")) {
            // 無效連結，直接回 ""
            return "";
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(pageUrl);
            conn = (HttpURLConnection) url.openConnection();

            // 模擬真實瀏覽器 UA
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.google.com");

            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            // 若 403，直接跳過
            if (responseCode == 403) {
                return "";
            }
            if (responseCode != 200) {
                return "";
            }

            // 3. 讀取內容
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            // 4. 放入 Cache
            String htmlContent = sb.toString();
            htmlCache.put(pageUrl, htmlContent);
            return htmlContent;
        } catch (IOException e) {
            // 失敗直接回 ""
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    } 

    /**
     * 用來儲存 root page 的結果(包含最終聚合分數)的內部類別
     */
    public class RootPageResult {
        private String title;
        private String url;
        private int aggregatedScore;
        private Map<String, String> scoreDetails; // 用於記錄分數細節
    
        public RootPageResult(String title, String url, int aggregatedScore, Map<String, String> scoreDetails) {
            this.title = title;
            this.url = url;
            this.aggregatedScore = aggregatedScore;
            this.scoreDetails = scoreDetails;
        }
    
        // Getter 和 Setter
        public String getTitle() {
            return title;
        }
    
        public String getUrl() {
            return url;
        }
    
        public int getAggregatedScore() {
            return aggregatedScore;
        }
    
        public Map<String, String> getScoreDetails() {
            return scoreDetails;
        }
    }    
}