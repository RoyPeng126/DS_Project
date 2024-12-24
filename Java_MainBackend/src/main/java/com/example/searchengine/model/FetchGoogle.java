package com.example.searchengine.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class FetchGoogle {

    private static final String API_URL = "http://femhv.ddns.net:5000/fetch";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> getrelate(String input) throws IOException {
        // 使用 HashMap 初始化請求參數
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", input);

        // 將 Map 轉換成 JSON 字符串
        String jsonRequestBody = objectMapper.writeValueAsString(requestBody);

        // 創建請求
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(jsonRequestBody, MediaType.parse("application/json")))
                .build();

        // 執行請求並處理響應
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 解析響應 JSON
            Map<String, Object> responseMap = objectMapper.readValue(response.body().string(), Map.class);
            return (List<String>) responseMap.get("keywords");
        }
    }
}
