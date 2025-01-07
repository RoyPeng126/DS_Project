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

    // 已拜訪過的 URL (防止重複抓)
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    
    // HTML Cache (避免重複下載同一頁)
    private final Map<String, String> htmlCache = new ConcurrentHashMap<>();
    
    // 執行緒池 (如要並行或做子頁抓取任務)
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 用於限制全域抓多少頁
    private final AtomicInteger totalPageCount = new AtomicInteger(0);
    // private static final int MAX_TOTAL_PAGES = 50;

    public KeywordCounterEngine() {
        // 預設建構子
    }

    /**
     * 重新搜尋前，可呼叫此方法清空相關計數與快取。
     * 避免因為 totalPageCount 與 visitedUrls 累積，導致之後都回傳 0 分。
     */
    public void resetEngineState() {
        visitedUrls.clear();
        htmlCache.clear();
        totalPageCount.set(0);
        logger.info("KeywordCounterEngine state has been reset.");
    }

    /**
     * 分析並取得某頁面的結構、關鍵字分數、以及(若 depth > 0)的子頁遞迴。
     */
    public Page getPageStructure(String htmlContent, List<Keyword> keywords, String title, String url, int depth) {
        logger.info("Processing page: {} , depth: {}, total pages processed so far: {}", url, depth, totalPageCount.get());

        // 若深度 < 0 或已達最大頁數，直接給 0 分
        if (depth < 0) {
            logger.info("Stop processing page: {} (depth: {} or max pages reached).", url, depth);
            return new Page(title, url, 0, new HashMap<>());
        }

        // 記數 +1
        totalPageCount.incrementAndGet();

        if (htmlContent == null || htmlContent.isEmpty()) {
            logger.warn("No content for URL: {}, score=0", url);
            return new Page(title, url, 0, new HashMap<>());
        }

        // 1. 分析關鍵字出現次數
        Map<Keyword, Integer> keywordOccurrences = analyzeOccurrences(htmlContent, keywords);
        logger.debug("Keyword occurrences in {}: {}", url, keywordOccurrences);

        // 2. 計算分數
        Map<String, String> scoreDetails = new HashMap<>();
        int score = calculateScore(keywordOccurrences, scoreDetails);
        logger.info("Score for page {}: {}", url, score);

        // 建立當前頁物件
        Page currentPage = new Page(title, url, score, scoreDetails);

        // 3. 若 depth > 0 再去抓子頁 (為確保速度，限定抓取一個子連結，以維持使用者體驗)
        if (depth > 0) {
            Document doc = Jsoup.parse(htmlContent);
            doc.setBaseUri(url); // 確保有 Base URI 設定

            Elements links = doc.select("a[href]");
            int childrenCount = 0;

            for (Element link : links) {
                if (childrenCount >= 1) {
                    break; // 只抓一個子連結
                }

                String childUrl = link.absUrl("href"); // 取得絕對 URL

                // 打印調試訊息，確認抓到的 URL
                System.out.println("Raw href: " + link.attr("href"));
                System.out.println("Calculated absolute URL: " + childUrl);

                if (childUrl.isEmpty()) {
                    System.out.println("Empty childUrl for link: " + link.text());
                    continue; // 跳過空的 URL
                }

                if (!childUrl.startsWith("http://") && !childUrl.startsWith("https://")) {
                    System.out.println("Invalid URL (not HTTP/HTTPS): " + childUrl);
                    continue; // 跳過非 HTTP/HTTPS 的連結
                }

                if (!isVisited(childUrl)) {
                    markVisited(childUrl);
                    childrenCount++;

                    System.out.printf("Fetching child page: %s%n", childUrl);
                    logger.debug("Fetching child page: {}", childUrl);

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

    /**
     * 分析關鍵字在 htmlContent 裡的出現次數
     */
    private Map<Keyword, Integer> analyzeOccurrences(String htmlContent, List<Keyword> keywords) {
        Map<Keyword, Integer> occurrences = new HashMap<>();
        Document doc = Jsoup.parse(htmlContent);

        // 取出 body text
        String textContent = (doc.body() != null) ? doc.body().text() : "";

        // 針對每個 keyword 做計算
        for (Keyword keyword : keywords) {
            String word = keyword.getWord();
            int count = countOccurrences(textContent, word);
            if (count > 0) {
                occurrences.put(keyword, count);
            }
        }
        return occurrences;
    }

    /**
     * 根據關鍵字出現次數 * 權重 來計算總分，並記錄計算細節
     */
    private int calculateScore(Map<Keyword, Integer> keywordOccurrences, Map<String, String> scoreDetails) {
        int totalScore = 0;
        for (Map.Entry<Keyword, Integer> entry : keywordOccurrences.entrySet()) {
            Keyword kw = entry.getKey();
            int occurrence = entry.getValue();
            double weight = kw.getWeight();
            int score = (int) (occurrence * weight);
            totalScore += score;

            // 把計算公式放到 scoreDetails
            scoreDetails.put(kw.getWord(), occurrence + " * " + weight + " = " + score);
        }
        return totalScore;
    }

    /**
     * 計算字串中關鍵字出現次數 (不使用單詞邊界)
     */
    private int countOccurrences(String text, String word) {
        Pattern pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 抓取網頁 (有簡單的 Cache 與 403/非200 處理)
     */
    private String fetchHtmlContent(String pageUrl) throws IOException {
        // 1. 檢查快取
        if (htmlCache.containsKey(pageUrl)) {
            return htmlCache.get(pageUrl);
        }

        // 2. 檢查協定
        if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://")) {
            return "";
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(pageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.google.com");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 403 || responseCode != 200) {
                return "";
            }

            // 3. 讀取內容
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            // 4. 放進 Cache
            String htmlContent = sb.toString();
            htmlCache.put(pageUrl, htmlContent);
            return htmlContent;
        } catch (IOException e) {
            logger.error("Error fetching page: {}", pageUrl, e);
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 檢查是否已訪問過該網址
     */
    private boolean isVisited(String url) {
        return visitedUrls.contains(url);
    }

    /**
     * 標記網址已訪問
     */
    private void markVisited(String url) {
        visitedUrls.add(url);
    }

    /**
     * 若要優雅關閉執行緒池，可在 Application 關閉前呼叫
     */
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
