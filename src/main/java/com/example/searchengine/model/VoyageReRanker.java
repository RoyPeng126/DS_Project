package com.example.searchengine.model;

import java.util.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class VoyageReRanker {

    private final Map<String, List<String>> keywordLists;

    public VoyageReRanker() {
        keywordLists = new HashMap<>();
        initializeKeywordLists();
    }

    private void initializeKeywordLists() {
        keywordLists.put("County/City", Arrays.asList(
                "臺北市", "新北市", "基隆市", "新竹市", "桃園市", "新竹縣", "宜蘭縣",
                "臺中市", "苗栗縣", "彰化縣", "南投縣", "雲林縣",
                "高雄市", "臺南市", "嘉義市", "嘉義縣", "屏東縣", "澎湖縣",
                "花蓮縣", "臺東縣", "金門縣", "連江縣"
        ));

        // TODO: 補全所有夜市名：https://zh.wikipedia.org/zh-tw/%E8%87%BA%E7%81%A3%E5%A4%9C%E5%B8%82%E5%88%97%E8%A1%A8#%E5%8C%97%E9%83%A8
        keywordLists.put("Night Market Name", Arrays.asList(
                "八斗子夜市", "碇內夜市", "士林夜市", "饒河夜市", "通化夜市", "逢甲夜市", "六合夜市"
        ));
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
