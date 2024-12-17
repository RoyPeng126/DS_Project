package com.example.searchengine.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GoogleQuery {
    public String searchKeyword;
    public String url;
    public String content;

    public GoogleQuery(String searchKeyword) {
        this.searchKeyword = searchKeyword;
        try {
            String encodeKeyword = java.net.URLEncoder.encode(searchKeyword, "utf-8");
            this.url = "https://www.google.com/search?q=" + encodeKeyword + "&oe=utf8&num=20";
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String fetchContent() throws IOException {
        StringBuilder retVal = new StringBuilder();

        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        conn.setRequestProperty("User-agent", "Chrome/107.0.5304.107");
        InputStream in = conn.getInputStream();

        InputStreamReader inReader = new InputStreamReader(in, "utf-8");
        BufferedReader bufReader = new BufferedReader(inReader);
        String line;

        while ((line = bufReader.readLine()) != null) {
            retVal.append(line);
        }
        return retVal.toString();
    }

    private String cleanUrl(String url) {
        int endIndex = url.indexOf("&");
        if (endIndex != -1) {
            return url.substring(0, endIndex);
        }
        return url;
    }

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

    public Map<String, String> query() throws IOException {
        if (content == null) {
            content = fetchContent();
        }

        Map<String, String> resultMap = new HashMap<>();
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select("div").select(".kCrYT");

        for (Element element : elements) {
            try {
                String citeUrl = element.select("a").get(0).attr("href").replace("/url?q=", "");
                citeUrl = cleanUrl(citeUrl);
                String title = element.select("a").get(0).select(".vvjwJb").text();

                if (title.isEmpty() || !isValidUrl(citeUrl)) {
                    continue;
                }

                resultMap.put(title, citeUrl);
            } catch (IndexOutOfBoundsException e) {
                // Ignore invalid elements
            }
        }

        return sortResults(resultMap);
    }

    private Map<String, String> sortResults(Map<String, String> unsortedMap) {
        List<Map.Entry<String, String>> list = new ArrayList<>(unsortedMap.entrySet());

        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                int length1 = list.get(j).getKey().length();
                int length2 = list.get(j + 1).getKey().length();

                if (length1 < length2) {
                    Collections.swap(list, j, j + 1);
                }
            }
        }

        Map<String, String> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
    
        return sortedMap;
    }    
}
