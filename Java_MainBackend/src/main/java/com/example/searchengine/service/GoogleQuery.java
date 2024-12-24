package com.example.searchengine.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleQuery {
    private static final Logger logger = LoggerFactory.getLogger(GoogleQuery.class);
    private static final int MAX_RESULTS = 10; 
    private static final int PAGE_SIZE = 10;

    private final String queryKeywords;

    public GoogleQuery(String queryKeywords) {
        this.queryKeywords = queryKeywords;
    }

    private String fetchContent(String query, int start) throws IOException {
        String apiKey = System.getenv("GOOGLE_CLOUD_API_KEY");
        String searchEngineId = System.getenv("GOOGLE_CLOUD_SEARCH_ENGINE_ID");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("未從環境變數中取得 GOOGLE_CLOUD_API_KEY");
        }
        if (searchEngineId == null || searchEngineId.isEmpty()) {
            throw new IOException("未從環境變數中取得 GOOGLE_CLOUD_SEARCH_ENGINE_ID");
        }

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
        conn.setConnectTimeout(5000);
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Custom Search API 回傳非 200 狀態碼: " + responseCode);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    public Map<String, String> query() throws IOException {
        Map<String, String> resultMap = new LinkedHashMap<>();
        int currentStart = 1;
        int totalFetched = 0;

        for (int pageIndex = 0; pageIndex < 5; pageIndex++) {
            String jsonString = fetchContent(queryKeywords, currentStart);

            JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);
            JsonArray items = jsonObject.has("items") ? jsonObject.getAsJsonArray("items") : null;

            if (items == null || items.size() == 0) {
                logger.warn("No items found in API response for query: {}", queryKeywords);
                break;
            }

            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String title = item.has("title") ? item.get("title").getAsString() : "";
                String link = item.has("link") ? item.get("link").getAsString() : "";

                if (!title.isEmpty() && !link.isEmpty() && isValidUrl(link)) {
                    resultMap.put(title, link);
                    totalFetched++;
                }

                if (totalFetched >= MAX_RESULTS) {
                    break;
                }
            }

            if (totalFetched >= MAX_RESULTS) {
                break;
            }
            currentStart += PAGE_SIZE;
        }

        return resultMap;
    }

    private boolean isValidUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(1000);
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            logger.warn("Invalid URL: {}", urlString);
            return false;
        }
    }
}
