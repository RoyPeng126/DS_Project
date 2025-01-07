# Final Project - Let's Beat Google!

## Topic

Snack Hunter: The Ultimate Taiwanese Night Market Food Finder

## Project introduction

[Project Proposal PDF](./DSproposal_Group9_final.pdf)

## TODO

[2025/01/07] Update this README.md: New section `Setup Environment`

[2024/12/31] "抓子網頁"改成預設: 開啟 + 不只抓一個 child (全抓)

[2024/12/26] Refactor the whole codebase (Frontend: APP, Web, Backend, Models, Docs)

## Source Code Structure

```
.
├── DSproposal_Group9_final.pdf
├── README.md
├── IOSAPP_Source
│   └── NightMarketSearch
│       ├── ContentView.swift
│       ├── Info.plist
│       ├── NightMarketSearchApp.swift
│       ├── SearchResult.swift
│       └── SearchViewModel.swift
├── Java_MainBackend
│   ├── output.csv
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── com
│   │       │       └── example
│   │       │           └── searchengine
│   │       │               ├── SearchEngineApplication.java
│   │       │               ├── controller
│   │       │               │   └── SearchController.java
│   │       │               ├── engine
│   │       │               │   ├── KeywordCounterEngine.java
│   │       │               │   └── KeywordExtractionEngine.java
│   │       │               ├── model
│   │       │               │   ├── CKIPTransformer.java
│   │       │               │   ├── ClassificationModel.java
│   │       │               │   ├── FetchGoogle.java
│   │       │               │   ├── Keyword.java
│   │       │               │   ├── KeywordExtractionResult.java
│   │       │               │   ├── Page.java
│   │       │               │   └── VoyageReRanker.java
│   │       │               └── service
│   │       │                   ├── GoogleQuery.java
│   │       │                   └── GoogleTranslateService.java
│   │       └── resources
│   │           ├── application.properties
│   │           ├── static
│   │           │   └── images
│   │           │       ├── bg.jpg
│   │           │       └── bg2.jpg
│   │           └── templates
│   │               └── index.html
├── python_backend
│   ├── fetch_google.py
│   ├── flask_app.py
│   └── test
│       └── test_api_search.py
```
