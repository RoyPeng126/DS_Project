package com.example.searchengine.model;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

public class VoyageReRanker {

    private final Map<String, List<String>> keywordLists;

    public VoyageReRanker() {
        keywordLists = new HashMap<>();
        initializeKeywordLists();
        loadKeywordsFromCSV("output.csv", "Night Market Name");
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

    public String findBestMatch(String input, String category) {
        List<String> candidates = keywordLists.getOrDefault(category, Collections.emptyList());
        return getBestMatchFromAPI(input, candidates);
    }

    private String getBestMatchFromAPI(String input, List<String> candidates) {
        String apiUrl = "http://127.0.0.1:5000/rerank";
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", input);
        requestBody.put("documents", candidates);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> rankedDocuments = (List<String>) response.getBody().get("ranked_documents");
                return rankedDocuments.isEmpty() ? null : rankedDocuments.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
