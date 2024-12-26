package com.example.searchengine.engine;

import com.example.searchengine.model.Keyword;
import com.example.searchengine.model.KeywordExtractionResult;
import com.example.searchengine.model.ClassificationModel;
import com.example.searchengine.model.CKIPTransformer;
import com.example.searchengine.service.GoogleTranslateService;
import com.example.searchengine.model.ClassificationModel.ClassificationResult;

import java.util.stream.Stream;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class KeywordExtractionEngine {

    private final ClassificationModel classificationModel;
    private final CKIPTransformer ckipTransformer;
    private final GoogleTranslateService googleTranslateService;

    public KeywordExtractionEngine(
            ClassificationModel classificationModel,
            CKIPTransformer ckipTransformer,
            GoogleTranslateService googleTranslateService
    ) {
        this.classificationModel = classificationModel;
        this.ckipTransformer = ckipTransformer;
        this.googleTranslateService = googleTranslateService;
    }

    public KeywordExtractionResult extractKeywords(String userInput) throws IOException {
        // 1. 檢查輸入是否非中文並進行翻譯 (設定一個 final boolean isTranslated)
        final boolean isTranslated;
        try {
            if (!isChinese(userInput)) {
                userInput = googleTranslateService.translateToChinese(userInput);
                isTranslated = true;
            } else {
                isTranslated = false;
            }
        } catch (IOException e) {
            e.printStackTrace(); 
            // 你也可以改用 logger 記錄，如 Logger.error("翻譯失敗", e);
            return new KeywordExtractionResult("", Collections.emptyList()); // 返回空結果
        }

        // 2. 檢查輸入是否包含空格 -> 決定要用自己簡單切詞 or CKIP
        List<String> tokens;
        if (userInput.contains(" ") || userInput.length() <= 8) {
            tokens = tokenize(userInput);
        } else {
            tokens = ckipTransformer.tokenize(userInput);
        }
        
        List<String> additionalKeywords = Arrays.asList("夜市", "美食", "店家");
        tokens = Stream.concat(tokens.stream(), additionalKeywords.stream())
                    .collect(Collectors.toList());

        // 3. 將分詞結果進行分類與權重分配
        List<Keyword> keywords = tokens.stream()
                .map(token -> categorizeAndAssignWeight(token, isTranslated))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 4. 組合關鍵字字串 (注意：這裡關鍵字已經有可能被替換為 matchedValue)
        String combinedKeywords = keywords.stream()
                .map(Keyword::getWord)
                .collect(Collectors.joining(" "));

        return new KeywordExtractionResult(combinedKeywords, keywords);
    }

    /**
     * 判斷字串中是否含有中文 (只要有一個字是中文就算)
     */
    private boolean isChinese(String input) {
        // 檢查每個 char 是否屬於 UnicodeScript.HAN
        return input.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
    }

    /**
     * 最簡單的用空格分詞
     */
    private List<String> tokenize(String input) {
        return Arrays.asList(input.split("\\s+"));
    }

    /**
     * 將 token 做分類 -> 回傳 Keyword
     * 若分數 < 0.9 則分類為 Food Name，matchedValue 為 null。
     * 否則若是 "County/City" 或 "Night Market Name"，matchedValue 可能有對應的正確名稱。
     *
     * 如果 userInput 是翻譯來的 (isTranslated = true)，且分類屬於縣市/夜市，我們就用 matchedValue 覆蓋 token
     * 以免 Google 翻譯的詞不精準。
     */
    private Keyword categorizeAndAssignWeight(String token, boolean isTranslated) {
        // 取得分類結果 (包含 category & matchedValue)
        ClassificationResult cr = classificationModel.predictCategory(token);
        if (cr == null) {
            // 若 model 回傳 null，直接忽略該 token
            return null;
        }

        String category = cr.getCategory();         // e.g. "County/City" / "Night Market Name" / "Food Name"
        String matchedValue = cr.getMatchedValue(); // 可能是 "臺北市"、"士林夜市"，或 null

        // 如果是縣市/夜市，權重不同；如果是其它 Food/未定義，也有預設權重
        double weight = getCategoryWeight(category);

        // 若翻譯過，且分類屬於縣市/夜市，而且 matchedValue != null，就用 matchedValue 覆蓋
        String finalWord = token;
        if (isTranslated && matchedValue != null 
                && (category.equals("County/City") || category.equals("Night Market Name"))) {
            finalWord = matchedValue;
        }

        return new Keyword(finalWord, weight);
    }

    /**
     * 根據類別指定一個權重
     */
    private double getCategoryWeight(String category) {
        switch (category) {
            case "County/City":
                return 3.0;
            case "Night Market Name":
                return 5.0;
            case "Food Name":
                return 15.0;
            default:
                return 15.0;
        }
    }
}
