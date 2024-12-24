package com.example.searchengine.model;

import com.example.searchengine.model.VoyageReRanker.BestMatchResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ClassificationModel 用於對單一 token 進行類別預測。
 * 可能的類別包含：
 * 1. County/City
 * 2. Night Market Name
 * 3. Food Name
 */
import org.springframework.stereotype.Component;

@Component
public class ClassificationModel {

    /**
     * 封裝分類結果：
     * - category: County/City、Night Market Name、Food Name
     * - matchedValue: 若是縣市或夜市，這裡會是實際匹配到的文字；若是 Food Name，為 null
     */
    public static class ClassificationResult {
        private final String category;       // 例如 "County/City"
        private final String matchedValue;   // 例如 "臺北市"

        public ClassificationResult(String category, String matchedValue) {
            this.category = category;
            this.matchedValue = matchedValue;
        }

        public String getCategory() {
            return category;
        }

        public String getMatchedValue() {
            return matchedValue;
        }

        @Override
        public String toString() {
            return "ClassificationResult{" +
                   "category='" + category + '\'' +
                   ", matchedValue='" + matchedValue + '\'' +
                   '}';
        }
    }

    private final VoyageReRanker voyageReRanker;
    private static final double THRESHOLD = 0.5;

    /**
     * 預設建構子：初始化 VoyageReRanker
     */
    public ClassificationModel() {
        this.voyageReRanker = new VoyageReRanker();
    }

    /**
     * 指定建構子：可傳入外部已建好的 VoyageReRanker 實例
     */
    public ClassificationModel(VoyageReRanker voyageReRanker) {
        this.voyageReRanker = voyageReRanker;
    }

    /**
     * 預測 token 所屬的類別。
     *
     * 1. 從 VoyageReRanker 取得「縣市」清單與「夜市」清單，合併為一個大清單
     * 2. 跟 token 做相似度比較，取出最佳匹配值與分數
     * 3. 若分數 < 0.9 則分類為 "Food Name"
     * 4. 若分數 >= 0.9，判斷其落在哪個清單中
     * 
     * @param token 欲分類的文字
     * @return ClassificationResult (包含 category 與 matchedValue)
     */
    public ClassificationResult predictCategory(String token) {
        // 1. 取出 "County/City" 和 "Night Market Name" 兩個清單
        Map<String, List<String>> keywordLists = voyageReRanker.getKeywordLists();
        List<String> cityList = keywordLists.getOrDefault("County/City", new ArrayList<>());
        List<String> nightMarketList = keywordLists.getOrDefault("Night Market Name", new ArrayList<>());

        // 2. 合併成一個大清單
        List<String> combinedList = new ArrayList<>();
        combinedList.addAll(cityList);
        combinedList.addAll(nightMarketList);

        // 3. 呼叫 VoyageReRanker，取得最佳匹配 (包含文字與分數)
        BestMatchResponse bestMatchResponse = voyageReRanker.getBestMatchWithScore(token, combinedList);
        if (bestMatchResponse == null) {
            // API 呼叫失敗或無結果，直接歸類為 Food Name
            return new ClassificationResult("Food Name", null);
        }

        String bestDoc = bestMatchResponse.getBestDocument();
        double bestScore = bestMatchResponse.getBestScore();

        // 若分數 < 0.9，歸類為 Food Name
        if (bestScore < THRESHOLD) {
            return new ClassificationResult("Food Name", null);
        }

        // 若分數 >= 0.9，判斷 bestDoc 屬於縣市或夜市
        if (cityList.contains(bestDoc)) {
            return new ClassificationResult("County/City", bestDoc);
        } else if (nightMarketList.contains(bestDoc)) {
            return new ClassificationResult("Night Market Name", bestDoc);
        } else {
            // 若都不屬於，預設為 Food Name
            return new ClassificationResult("Food Name", null);
        }
    }
}
