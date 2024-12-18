package com.example.searchengine.engine;

import com.example.searchengine.model.Keyword;
import com.example.searchengine.model.Page;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordCounterEngine {

    private Set<String> visitedUrls = new HashSet<>();

    public KeywordCounterEngine() {
        // 建構子
    }

    /**
     * 構建頁面結構樹並計算關鍵字分數。
     * 
     * @param htmlContent 主頁面的 HTML 內容
     * @param keywords    關鍵字列表
     * @param title       主頁面的標題
     * @param url         主頁面的 URL
     * @param depth       爬取的最大深度
     * @return 樹的根節點 (Page 物件)
     */
    public Page getPageStructure(String htmlContent, List<Keyword> keywords, String title, String url, int depth) {
        if (depth == 0) {
            return null; // 遞迴停止條件
        }

        // 第一步：取得關鍵字出現次數
        Map<Keyword, Integer> keywordOccurrences = analyzeOccurrences(htmlContent, keywords);

        // 第二步：計算分數
        int score = calculateScore(keywordOccurrences);

        // 建立本頁面的 Page 節點
        Page currentPage = new Page(title, url, score);

        // 處理子頁面
        Document doc = Jsoup.parse(htmlContent);
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String childUrl = link.absUrl("href");

            // 避免循環引用並檢查 URL 有效性
            if (!isVisited(childUrl) && isValidUrl(childUrl)) {
                try {
                    markVisited(childUrl); // 標記 URL
                    String childHtmlContent = fetchHtmlContent(childUrl); // 抓取子頁面的 HTML

                    // 遞迴獲取子頁面的結構，深度減 1
                    Page childPage = getPageStructure(childHtmlContent, keywords, link.text(), childUrl, depth - 1);

                    if (childPage != null) {
                        currentPage.addChild(childPage); // 將子頁面加入樹中
                    }
                } catch (IOException e) {
                    System.err.println("Failed to fetch child page: " + childUrl);
                    e.printStackTrace();
                }
            }
        }

        return currentPage; // 返回樹的根節點
    }

    /**
     * 分析頁面中所有 keyword 的出現次數。
     */
    private Map<Keyword, Integer> analyzeOccurrences(String htmlContent, List<Keyword> keywords) {
        Map<Keyword, Integer> occurrences = new HashMap<>();
        for (Keyword keyword : keywords) {
            occurrences.put(keyword, 0);
        }

        Document doc = Jsoup.parse(htmlContent);
        String textContent = doc.body().text();

        for (Keyword keyword : keywords) {
            String word = keyword.getWord();
            int count = countOccurrences(textContent, word);
            occurrences.put(keyword, occurrences.get(keyword) + count);
        }
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

    /**
     * 計算文本中某個單詞的出現次數。
     */
    private int countOccurrences(String text, String word) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 檢查 URL 是否有效。
     */
    private boolean isValidUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.connect();
            return (conn.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 抓取網頁 HTML 內容。
     */
    private String fetchHtmlContent(String pageUrl) throws IOException {
        StringBuilder sb = new StringBuilder();
        URL u = new URL(pageUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("User-Agent", "Chrome/107.0.5304.107");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 檢查是否已訪問 URL。
     */
    private boolean isVisited(String url) {
        return visitedUrls.contains(url);
    }

    /**
     * 標記 URL 為已訪問。
     */
    private void markVisited(String url) {
        visitedUrls.add(url);
    }
}
