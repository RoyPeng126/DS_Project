package com.example.searchengine.controller;

import com.example.searchengine.service.GoogleQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

@Controller
public class SearchController {

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
        GoogleQuery googleQuery = new GoogleQuery(query);
        try {
            Map<String, String> results = googleQuery.query();
            model.addAttribute("results", results);
            model.addAttribute("query", query);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Error fetching results");
        }
        return "index";
    }
}
