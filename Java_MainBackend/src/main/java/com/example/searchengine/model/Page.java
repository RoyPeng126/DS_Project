package com.example.searchengine.model;

import java.util.ArrayList;
import java.util.List;

public class Page {
    private String title;
    private String url;
    private int score; // 此分數為該頁面本身的分數(不含子頁)
    private List<Page> children;

    public Page(String title, String url, int score) {
        this.title = title;
        this.url = url;
        this.score = score;
        this.children = new ArrayList<>();
    }

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
}
