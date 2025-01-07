package com.example.searchengine.model;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import org.springframework.stereotype.Component;

@Component
public class VoyageReRanker {

    // 用來封裝“最佳匹配文字”與“對應分數”的資料結構
    public static class BestMatchResponse {
        private final String bestDocument;
        private final double bestScore;

        public BestMatchResponse(String bestDocument, double bestScore) {
            this.bestDocument = bestDocument;
            this.bestScore = bestScore;
        }

        public String getBestDocument() {
            return bestDocument;
        }

        public double getBestScore() {
            return bestScore;
        }
    }

    private final Map<String, List<String>> keywordLists;

    public VoyageReRanker() {
        keywordLists = new HashMap<>();
        initializeKeywordLists();
        loadKeywordsFromCSV("nightmarket_info.csv", "Night Market Name");
    }

    public Map<String, List<String>> getKeywordLists() {
        return keywordLists;
    }

    private void initializeKeywordLists() {
        keywordLists.put("County/City", Arrays.asList(
                "臺北市", "新北市", "基隆市", "新竹市", "桃園市", "新竹縣", "宜蘭縣",
                "臺中市", "苗栗縣", "彰化縣", "南投縣", "雲林縣",
                "高雄市", "臺南市", "嘉義市", "嘉義縣", "屏東縣", "澎湖縣",
                "花蓮縣", "臺東縣", "金門縣", "連江縣"
        ));
    }

    private void loadKeywordsFromCSV(String filePath, String category) {
        try (Reader reader = Files.newBufferedReader(Paths.get(filePath));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

            List<String> keywords = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                String keyword = record.get(0).trim(); // 取每行的第一列
                if (!keyword.isEmpty()) {
                    keywords.add(keyword);
                }
            }
            keywordLists.put(category, keywords);
            System.out.println("成功加載 " + keywords.size() + " 個關鍵字到類別: " + category);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 核心函式：傳入輸入字串 (input) 與候選文檔 (candidates)，呼叫 Python API 進行相似度比較，
     * 回傳「最佳匹配文字 + 分數」。
     */
    public BestMatchResponse getBestMatchWithScore(String input, List<String> candidates) {
        String apiUrl = "http://localhost:5000/rerank";
        RestTemplate restTemplate = new RestTemplate();
    
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", input);
        requestBody.put("documents", candidates);
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
    
        try {
            // e.g.
            // {
            //   "ranked_documents": [
            //     {"document": "臺中市", "score": 0.95},
            //     {"document": "彰化縣", "score": 0.85}
            //   ]
            // }
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> rankedDocuments = 
                    (List<Map<String, Object>>) response.getBody().get("ranked_documents");
    
                if (rankedDocuments != null && !rankedDocuments.isEmpty()) {
                    // 取出第一個文檔及其分數
                    Map<String, Object> bestMatch = rankedDocuments.get(0);
                    String bestDoc = (String) bestMatch.get("document");
                    double bestScore = ((Number) bestMatch.get("score")).doubleValue();
    
                    return new BestMatchResponse(bestDoc, bestScore);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 若呼叫失敗或無匹配結果則回傳 null
    }    
}
