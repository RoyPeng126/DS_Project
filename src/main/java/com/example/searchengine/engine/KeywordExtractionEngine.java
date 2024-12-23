package com.example.searchengine.engine;

import com.example.searchengine.model.Keyword;
import com.example.searchengine.model.KeywordExtractionResult;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import com.example.searchengine.model.ClassificationModel;
import com.example.searchengine.model.VoyageReRanker;
import com.example.searchengine.model.CKIPTransformer;
import com.example.searchengine.service.GoogleTranslateService;

public class KeywordExtractionEngine {

    private final ClassificationModel classificationModel;
    private final VoyageReRanker voyageReRanker;
    private final CKIPTransformer ckipTransformer;
    private final GoogleTranslateService googleTranslateService;

    public KeywordExtractionEngine(ClassificationModel classificationModel, VoyageReRanker voyageReRanker, CKIPTransformer ckipTransformer, GoogleTranslateService googleTranslateService) {
        this.classificationModel = classificationModel;
        this.voyageReRanker = voyageReRanker;
        this.ckipTransformer = ckipTransformer;
        this.googleTranslateService = googleTranslateService;
    }

    public KeywordExtractionResult extractKeywords(String userInput) throws IOException {
        // 1. 檢查輸入是否非中文並進行翻譯
        boolean translated = false;
        try {
            if (!isChinese(userInput)) {
                userInput = googleTranslateService.translateToChinese(userInput);
                translated = true;
            }
        } catch (IOException e) {
            e.printStackTrace(); // 可以改用日誌記錄，例如：Logger.error("翻譯失敗", e);
            return new KeywordExtractionResult("", Collections.emptyList()); // 返回空結果
        }
    
        // 2. 檢查輸入是否包含空格
        List<String> tokens;
        if (userInput.contains(" ")) {
            tokens = tokenize(userInput);
        } else {
            tokens = ckipTransformer.tokenize(userInput);
        }

        // 3. 將分詞結果進行分類與權重分配
        List<Keyword> keywords = tokens.stream()
                .map(this::categorizeAndAssignWeight)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 4. 替換城市與夜市名關鍵字（僅在翻譯過的情況下執行）
        keywords = translated ? 
                keywords.stream()
                        .map(this::replaceWithBestMatch)
                        .collect(Collectors.toList()) : keywords;
    
        // 5. 組合關鍵字字串
        String combinedKeywords = keywords.stream()
                .map(Keyword::getWord)
                .collect(Collectors.joining(" "));
    
        return new KeywordExtractionResult(combinedKeywords, keywords);
    }    

    private boolean isChinese(String input) {
        return input.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
    }

    private List<String> tokenize(String input) {
        // 使用空格進行分詞
        return Arrays.asList(input.split("\\s+"));
    }

    private Keyword categorizeAndAssignWeight(String token) {
        String category = classificationModel.predictCategory(token);
        if (category == null) {
            return null;
        }
        double weight = getCategoryWeight(category);
        return new Keyword(token, weight);
    }

    private double getCategoryWeight(String category) {
        switch (category) {
            case "County/City":
                return 5.0;
            case "Night Market Name":
                return 10.0;
            case "Food Type":
                return 8.0;
            case "Food Name":
                return 8.0;
            case "Others":
            default:
                return 0.0;
        }
    }

    private Keyword replaceWithBestMatch(Keyword keyword) {
        String category = classificationModel.predictCategory(keyword.getWord());
        if (category == null || (!category.equals("County/City") && !category.equals("Night Market Name"))) {
            return keyword;
        }

        String bestMatch = voyageReRanker.findBestMatch(keyword.getWord(), category);
        if (bestMatch != null) {
            return new Keyword(bestMatch, keyword.getWeight());
        }
        return keyword;
    }
}
