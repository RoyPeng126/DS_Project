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
    private static final int MAX_INVALID_URLS = 5; // 最大無效 URL 次數
    private final AtomicInteger invalidUrlCount = new AtomicInteger(0);
    private final Map<String, Boolean> urlValidityCache = new ConcurrentHashMap<>();
    private final Set<String> problematicUrls = ConcurrentHashMap.newKeySet(); // 記錄有問題的 URL

    public KeywordCounterEngine() {
        // 建構子
    }

    public Page getPageStructure(String htmlContent, List<Keyword> keywords, String title, String url, int depth) {
        logger.info("Processing page: {} with depth: {} and total pages processed: {}", url, depth, totalPageCount.get());
    
        if (depth <= 0 || htmlContent.isEmpty() || totalPageCount.get() >= MAX_TOTAL_PAGES) {
            logger.info("Stopping recursion for page: {} due to depth: {} or max pages reached: {}", url, depth, totalPageCount.get());
            return null; // 停止條件
        }
    
        totalPageCount.incrementAndGet(); // 計數器增量
    
        // 第一步：取得關鍵字出現次數
        Map<Keyword, Integer> keywordOccurrences = analyzeOccurrences(htmlContent, keywords);
        logger.debug("Keyword occurrences for page {}: {}", url, keywordOccurrences);
    
        // 第二步：計算分數
        int score = calculateScore(keywordOccurrences);
        logger.info("Score for page {}: {}", url, score);
    
        // 建立本頁面的 Page 節點
        Page currentPage = new Page(title, url, score);
    
        // 處理子頁面，限制最多抓取 1 個子頁面
        Document doc = Jsoup.parse(htmlContent);
        Elements links = doc.select("a[href]");
    
        List<Future<Page>> futures = new ArrayList<>();
        int childrenCount = 0;
        for (Element link : links) {
            if (childrenCount >= 1 || totalPageCount.get() >= MAX_TOTAL_PAGES) break;
    
            String childUrl = link.absUrl("href");
            if (!isVisited(childUrl) && isValidUrlWithRetry(childUrl, 3)) {
                logger.info("Fetching child page: {}", childUrl);
                markVisited(childUrl);
                childrenCount++;
                futures.add(executorService.submit(() -> {
                    try {
                        String childHtmlContent = fetchHtmlContentWithRetry(childUrl, 3);
                        if (childHtmlContent == null || childHtmlContent.isEmpty()) {
                            logger.warn("Failed to fetch content for child page: {}", childUrl);
                            return null;
                        }
                        return getPageStructure(childHtmlContent, keywords, link.text(), childUrl, depth - 1);
                    } catch (Exception e) {
                        logger.error("Error fetching child page: {}", childUrl, e);
                        return null;
                    }
                }));
            } else {
                logger.debug("Skipping child page: {} (already visited or invalid)", childUrl);
            }
        }
    
        for (Future<Page> future : futures) {
            try {
                Page childPage = future.get(2, TimeUnit.SECONDS); // 限制等待時間
                if (childPage != null) {
                    currentPage.addChild(childPage);
                }
            } catch (TimeoutException e) {
                logger.warn("Child page processing timed out");
                future.cancel(true);
            } catch (Exception e) {
                logger.error("Error processing child page future", e);
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

    private boolean isValidUrlWithRetry(String urlString, int retries) {
        for (int i = 0; i < retries; i++) {
            try {
                return isValidUrl(urlString);
            } catch (Exception e) {
                logger.warn("Retrying URL validation for: {} (attempt: {})", urlString, i + 1);
            }
        }
        return false;
    }

    private boolean isValidUrl(String urlString) {
        if (problematicUrls.contains(urlString)) {
            logger.warn("Skipping problematic URL: {}", urlString);
            return false;
        }
        if (urlValidityCache.containsKey(urlString)) {
            return urlValidityCache.get(urlString);
        }
        if (urlString.contains("NEWSLETTER") || urlString.endsWith(".pdf")) {
            logger.warn("Skipping known invalid URL: {}", urlString);
            urlValidityCache.put(urlString, false);
            return false;
        }
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(1000);
            conn.connect();
            boolean isValid = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            urlValidityCache.put(urlString, isValid);
            return isValid;
        } catch (IOException e) {
            int currentInvalid = invalidUrlCount.incrementAndGet();
            if (currentInvalid >= MAX_INVALID_URLS) {
                logger.error("Exceeded maximum invalid URL attempts: {}, stopping execution", MAX_INVALID_URLS);
                shutdown();
                throw new RuntimeException("Too many invalid URLs, stopping execution.");
            }
            logger.warn("Invalid URL: {}", urlString);
            problematicUrls.add(urlString);
            urlValidityCache.put(urlString, false);
            return false;
        }
    }

    private String fetchHtmlContentWithRetry(String pageUrl, int retries) throws IOException {
        IOException lastException = null;
        for (int i = 0; i < retries; i++) {
            try {
                return fetchHtmlContent(pageUrl);
            } catch (IOException e) {
                lastException = e;
                logger.warn("Retrying to fetch content for URL: {} (attempt: {})", pageUrl, i + 1);
                try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted during sleep", ex);
            } // 避免過於頻繁的重試
            }
        }
        throw lastException;
    }

    private String fetchHtmlContent(String pageUrl) throws IOException {
        if (problematicUrls.contains(pageUrl)) {
            logger.warn("Skipping problematic URL: {}", pageUrl);
            throw new IOException("Problematic URL skipped: " + pageUrl);
        }

        if (htmlCache.containsKey(pageUrl)) {
            logger.debug("Using cached content for URL: {}", pageUrl);
            return htmlCache.get(pageUrl); // 使用緩存結果
        }

        StringBuilder sb = new StringBuilder();
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
        } catch (IOException e) {
            problematicUrls.add(pageUrl);
            logger.error("Failed to fetch content for URL: {}", pageUrl, e);
            throw e;
        }

        String htmlContent = sb.toString();
        htmlCache.put(pageUrl, htmlContent); // 緩存 HTML 結果
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
    }
}
