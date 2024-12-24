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
    private static final int MAX_TOTAL_PAGES = 10; // 最大處理頁面數量

    public KeywordCounterEngine() {
        // 建構子
    }

    public Page getPageStructure(String htmlContent, List<Keyword> keywords, String title, String url, int depth) {
        logger.info("Processing page: {} with depth: {} and total pages processed: {}", url, depth, totalPageCount.get());

        if (depth <= 0 || totalPageCount.get() >= MAX_TOTAL_PAGES) {
            logger.info("Stopping recursion for page: {} due to depth: {} or max pages reached: {}", url, depth, totalPageCount.get());
            return new Page(title, url, 0); // 停止條件，返回默认 Page
        }

        totalPageCount.incrementAndGet(); // 計數器增量

        // 第一步：抓取 HTML 內容
        if (htmlContent == null || htmlContent.isEmpty()) {
            htmlContent = fetchHtmlContent(url);
        }

        if (htmlContent == null || htmlContent.isEmpty()) {
            logger.warn("Failed to fetch or process content for URL: {}, assigning score 0", url);
            return new Page(title, url, 0); // HTML 無法取得，分數設為 0
        }

        // 第二步：取得關鍵字出現次數
        Map<Keyword, Integer> keywordOccurrences = analyzeOccurrences(htmlContent, keywords);
        logger.debug("Keyword occurrences for page {}: {}", url, keywordOccurrences);

        // 第三步：計算分數
        int score = calculateScore(keywordOccurrences);
        logger.info("Score for page {}: {}", url, score);

        // 建立本頁面的 Page 節點
        Page currentPage = new Page(title, url, score);

        // 處理子頁面，限制最多抓取 1 個子頁面
        Document doc = Jsoup.parse(htmlContent);
        Elements links = doc.select("a[href]");

        int childrenCount = 0;
        for (Element link : links) {
            if (childrenCount >= 1 || totalPageCount.get() >= MAX_TOTAL_PAGES) break;

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

        return currentPage; // 返回樹的根節點
    }

    private Map<Keyword, Integer> analyzeOccurrences(String htmlContent, List<Keyword> keywords) {
        Map<Keyword, Integer> occurrences = new HashMap<>();
        Document doc = Jsoup.parse(htmlContent);
        String textContent = doc.body() != null ? doc.body().text() : "";

        for (Keyword keyword : keywords) {
            String word = keyword.getWord();
            int count = countOccurrences(textContent, word);
            if (count > 0) {
                occurrences.put(keyword, count);
            }
        }
        return occurrences;
    }

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

    private int countOccurrences(String text, String word) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String fetchHtmlContent(String pageUrl) {
        StringBuilder sb = new StringBuilder();
        try {
            // 检查协议是否存在
            if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://")) {
                logger.warn("Skipping URL due to missing protocol: {}", pageUrl);
                return ""; // 返回空内容
            }

            URL u = new URL(pageUrl);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestProperty("User-Agent", "Chrome/107.0.5304.107");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            logger.error("Failed to fetch content for URL: {}, assigning empty content", pageUrl, e);
            return ""; // 返回空内容
        }
        String htmlContent = sb.toString();
        logger.debug("Fetched content for URL: {} (length: {})", pageUrl, htmlContent.length());
        return htmlContent;
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
