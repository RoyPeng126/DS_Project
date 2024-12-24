package com.example.searchengine.engine;

import com.example.searchengine.model.Keyword;
import com.example.searchengine.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeywordCounterEngine {

    private static final Logger logger = LoggerFactory.getLogger(KeywordCounterEngine.class);
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Map<String, String> htmlCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 降低執行緒數量以減少伺服器壓力
    private final AtomicInteger totalPageCount = new AtomicInteger(0);
    private static final int MAX_TOTAL_PAGES = 50; // 最大處理頁面數量

    public KeywordCounterEngine() {
        // 建構子
    }

    public Page getPageStructure(String htmlContent, List<Keyword> keywords, String title, String url, int depth) {
        logger.info("Processing page: {} with depth: {} and total pages processed: {}", url, depth, totalPageCount.get());

        // 如果 depth < 0 或者總頁數已達上限，就不處理任何東西
        if (depth < 0 || totalPageCount.get() >= MAX_TOTAL_PAGES) {
            logger.info("Stopping recursion for page: {} due to depth: {} or max pages reached: {}", url, depth, totalPageCount.get());
            System.out.printf("Stopping recursion for page: {} due to depth: {} or max pages reached: {}", url, depth, totalPageCount.get());
            return new Page(title, url, 0, new HashMap<>()); 
        }

        // 不管 depth 是 0 還是 1、2...，先對「本頁」做分析
        totalPageCount.incrementAndGet(); 

        if (htmlContent == null || htmlContent.isEmpty()) {
            logger.warn("Failed to fetch or process content for URL: {}, assigning score 0", url);
            System.out.printf("Failed to fetch or process content for URL: {}, assigning score 0", url);
            return new Page(title, url, 0, new HashMap<>());
        }

        // 1. 分析關鍵字
        Map<Keyword, Integer> keywordOccurrences = analyzeOccurrences(htmlContent, keywords);
        // System.out.println("Keyword 出現次數: " + keywordOccurrences);
        logger.debug("Keyword occurrences for page {}: {}", url, keywordOccurrences);

        // 2. 計算分數
        Map<String, String> scoreDetails = new HashMap<>();
        int score = calculateScore(keywordOccurrences, scoreDetails);

        logger.info("Score for page {}: {}", url, score);

        // 建立根 Page
        Page currentPage = new Page(title, url, score, scoreDetails);

        // 3. 若 depth > 0 才處理子頁面 (代表可以繼續往下抓)
        if (depth > 0) {
            Document doc = Jsoup.parse(htmlContent);
            Elements links = doc.select("a[href]");
            int childrenCount = 0;

            for (Element link : links) {
                if (childrenCount >= 1 || totalPageCount.get() >= MAX_TOTAL_PAGES) {
                    break;
                }

                String childUrl = link.absUrl("href");
                if (!isVisited(childUrl)) {
                    logger.info("Fetching child page: {}", childUrl);
                    markVisited(childUrl);
                    childrenCount++;

                    try {
                        String childHtmlContent = fetchHtmlContent(childUrl);
                        Page childPage = getPageStructure(childHtmlContent, keywords, link.text(), childUrl, depth - 1);
                        if (childPage != null) {
                            currentPage.addChild(childPage);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing child page: {}", childUrl, e);
                    }
                } else {
                    logger.debug("Skipping child page: {} (already visited)", childUrl);
                }
            }
        }

        return currentPage;
    }

    private Map<Keyword, Integer> analyzeOccurrences(String htmlContent, List<Keyword> keywords) {
        Map<Keyword, Integer> occurrences = new HashMap<>();
        Document doc = Jsoup.parse(htmlContent);
        String textContent = doc.body() != null ? doc.body().text() : "";
    
        // System.out.println("Analyzing Text Content: " + textContent);
    
        for (Keyword keyword : keywords) {
            String word = keyword.getWord();
            // System.out.println("Checking keyword: " + word);
            int count = countOccurrences(textContent, word);
            if (count > 0) {
                occurrences.put(keyword, count);
            }
            // System.out.println("Count for keyword '" + word + "': " + count);
        }
        return occurrences;
    }    

    private int calculateScore(Map<Keyword, Integer> keywordOccurrences, Map<String, String> scoreDetails) {
        int totalScore = 0;
        for (Map.Entry<Keyword, Integer> entry : keywordOccurrences.entrySet()) {
            Keyword kw = entry.getKey();
            int occurrence = entry.getValue();
            double weight = kw.getWeight();
            int score = (int) (occurrence * weight);
            totalScore += score;
    
            // 保存計算細節到 scoreDetails
            scoreDetails.put(kw.getWord(), occurrence + " * " + weight + " = " + score);
        }
        return totalScore;
    }    

    private int countOccurrences(String text, String word) {
        // 直接匹配關鍵字，不使用單詞邊界
        Pattern pattern = Pattern.compile(Pattern.quote(word));
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
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

    private boolean isVisited(String url) {
        return visitedUrls.contains(url);
    }

    private void markVisited(String url) {
        visitedUrls.add(url);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
