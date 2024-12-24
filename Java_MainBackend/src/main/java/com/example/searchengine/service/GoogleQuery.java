package com.example.searchengine.service;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

import com.example.searchengine.model.FetchGoogle;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 接收關鍵字字串 (空白分隔)，向 Google Cloud Custom Search API 搜尋，
 * 一次最多 10 筆，最多呼叫 5 次以取得前 50 筆結果 (title -> url)。
 *
 * 請先於環境變數中設置：
 *   - GOOGLE_CLOUD_API_KEY
 *   - GOOGLE_CLOUD_SEARCH_ENGINE_ID
 */
public class GoogleQuery {
    private static final int MAX_RESULTS = 50; // 最多要抓幾筆
    private static final int PAGE_SIZE = 10;   // 一次可抓幾筆，Custom Search API 預設為 10

    private String queryKeywords;

    public GoogleQuery(String queryKeywords) {
        this.queryKeywords = queryKeywords;
    }

    /**
     * 呼叫一次 API，從指定的 start 值開始抓 10 筆結果
     * @param query 搜尋關鍵字
     * @param start 第幾筆開始 (1-based)
     * @return 回傳 API 回應的 JSON 字串
     */
    private String fetchContent(String query, int start) throws IOException {
        // 從環境變數讀取 API_KEY 及 SEARCH_ENGINE_ID
        String apiKey = System.getenv("GOOGLE_CLOUD_API_KEY");
        String searchEngineId = System.getenv("GOOGLE_CLOUD_SEARCH_ENGINE_ID");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("未從環境變數中取得 GOOGLE_CLOUD_API_KEY");
        }
        if (searchEngineId == null || searchEngineId.isEmpty()) {
            throw new IOException("未從環境變數中取得 GOOGLE_CLOUD_SEARCH_ENGINE_ID");
        }

        // 這裡不使用 num 參數，而是透過預設(10筆)，再用start分頁來抓取
        String urlString = String.format(
            "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&start=%d",
            URLEncoder.encode(apiKey, "UTF-8"),
            URLEncoder.encode(searchEngineId, "UTF-8"),
            URLEncoder.encode(query, "UTF-8"),
            start
        );

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Custom Search API 回傳非 200 狀態碼: " + responseCode);
        }

        // 讀取 API 回應
        StringBuilder sb = new StringBuilder();
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8")
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    /**
     * 使用 HtmlUnit 進行實際的 Google Search，並解析載入後的 HTML。
     * @param query 關鍵字
     * @return 回傳在 class="y6Uyqe" 區塊內所有文字，逐行裝在 List<String>
     */
    public List<String> fetchGoogleResultText(String query) {
        try {
            // 建立 FetchGoogle 物件 (若您有使用 Spring 管理，可改用 @Autowired 來注入)
            FetchGoogle fetchGoogle = new FetchGoogle();
            // 呼叫 fetchGoogle.getrelate(...) 並回傳取得的 List<String>
            return fetchGoogle.getrelate(query);
        } catch (IOException e) {
            // 可視需求做更進一步處理或丟出自訂例外
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 對外提供的搜尋方法：回傳 Map<String, String>，對應 title -> url
     * 最多抓取 50 筆(5 頁)結果。
     */
    public Map<String, String> query() throws IOException {
        Map<String, String> resultMap = new LinkedHashMap<>();

        // 最多 5 次呼叫，每次 10 筆
        int currentStart = 1; // 第一次從1開始
        int totalFetched = 0;

        for (int pageIndex = 0; pageIndex < 5; pageIndex++) {
            String jsonString = fetchContent(queryKeywords, currentStart);

            // 解析 JSON
            JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);
            JsonArray items = jsonObject.getAsJsonArray("items");

            // 如果本次沒有拿到結果，就提早結束
            if (items == null || items.size() == 0) {
                break;
            }

            // 處理本次取得的 items
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String title = item.has("title") ? item.get("title").getAsString() : "";
                String link = item.has("link") ? item.get("link").getAsString() : "";
                String snippet = item.has("snippet") ? item.get("snippet").getAsString() : "";
                String combine = title + "DSPROJECT/x01" + snippet;

                if (!title.isEmpty() && !link.isEmpty()) {
                    resultMap.put(combine, link);
                    totalFetched++;
                }

                // 若已累積到 50 筆就停止
                if (totalFetched >= MAX_RESULTS) {
                    break;
                }
            }

            // 若已累積到 50 筆就停止，不再呼叫下一頁
            if (totalFetched >= MAX_RESULTS) {
                break;
            }

            // 更新下一頁的起始值 (預設一次返回10筆)
            currentStart += PAGE_SIZE;
        }

        return resultMap;
    }
}