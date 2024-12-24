package com.example.searchengine.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

import org.springframework.stereotype.Component;

@Component
public class GoogleTranslateService {

    private static final String API_KEY = System.getenv("GOOGLE_CLOUD_API_KEY");
    private static final String TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String DETECT_URL = "https://translation.googleapis.com/language/translate/v2/detect";

    public String detectLanguage(String text) throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("Google Translate API key is not set.");
        }

        URL url = new URL(DETECT_URL + "?key=" + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String requestBody = new JSONObject()
                .put("q", text)
                .toString();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        Scanner scanner = new Scanner(connection.getInputStream());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject responseJson = new JSONObject(response);
        return responseJson.getJSONObject("data")
                .getJSONArray("detections")
                .getJSONArray(0)
                .getJSONObject(0)
                .getString("language");
    }

    public String translateToChinese(String text) throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("Google Translate API key is not set.");
        }

        // Step 1: Detect the language
        String detectedLanguage = detectLanguage(text);

        // Step 2: Check if the language is Traditional Chinese (zh-TW)
        if ("zh-TW".equals(detectedLanguage)) {
            return text; // No translation needed
        }

        // Step 3: Translate to Traditional Chinese
        URL url = new URL(TRANSLATE_URL + "?key=" + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String requestBody = new JSONObject()
                .put("q", text)
                .put("source", detectedLanguage)
                .put("target", "zh-TW")
                .toString();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        Scanner scanner = new Scanner(connection.getInputStream());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject responseJson = new JSONObject(response);
        return responseJson.getJSONObject("data")
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("translatedText");
    }
}
