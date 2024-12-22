// GoogleTranslateService.java
package com.example.searchengine.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

public class GoogleTranslateService {

    private static final String API_KEY = System.getenv("GOOGLE_TRANSLATE_API_KEY");
    private static final String TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";

    public String translateToChinese(String text) throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("Google Translate API key is not set.");
        }

        URL url = new URL(TRANSLATE_URL + "?key=" + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String requestBody = new JSONObject()
                .put("q", text)
                .put("target", "zh")
                .toString();

        connection.getOutputStream().write(requestBody.getBytes());

        Scanner scanner = new Scanner(connection.getInputStream());
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        JSONObject responseJson = new JSONObject(response);
        return responseJson.getJSONObject("data").getJSONArray("translations")
                .getJSONObject(0).getString("translatedText");
    }
}
