# Final Project - Let's Beat Google!

## Topic

Snack Hunter: The Ultimate Taiwanese Night Market Food Finder

## Project introduction

[Project Proposal PDF](./DSproposal_Group9_final.pdf)

## TODO

[2025/01/07] Update this README.md: New section `Setup Environment`

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
│   ├── nightmarket_info.csv
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
│   │           └── templates
│   │               └── index.html
├── python_backend
│   ├── fetch_google.py
│   ├── flask_app.py
│   └── test
│       └── test_api_search.py
```
