package com.example.searchengine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Page {
    private String title;
    private String url;
    private int score; // 此分數為該頁面本身的分數(不含子頁)
    private List<Page> children;
    private Map<String, String> scoreDetails; // 保存分數計算細節

    public Page(String title, String url, int score, Map<String, String> scoreDetails) {
        this.title = title;
        this.url = url;
        this.score = score;
        this.scoreDetails = scoreDetails;
        this.children = new ArrayList<>();
    }

    // Getter 和 Setter
    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public int getScore() {
        return score;
    }

    public List<Page> getChildren() {
        return children;
    }

    public void addChild(Page child) {
        this.children.add(child);
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Map<String, String> getScoreDetails() {
        return scoreDetails;
    }

    public void setScoreDetails(Map<String, String> scoreDetails) {
        this.scoreDetails = scoreDetails;
    }
}
