# Final Project - Let's Beat Google!

## Topic

Snack Hunter: The Ultimate Taiwanese Night Market Food Finder

## Project introduction

[Project Proposal PDF](./DSproposal_Group9_final.pdf)

## TODO

[2025/01/07] Update this README.md: New section `Setup Environment`

[2024/12/31] "抓子網頁"改成預設: 開啟 + 不只抓一個 child (全抓)

[2024/12/26] Refactor the whole codebase (Frontend: APP, Web, Backend, Models, Docs)

[2024/12/25] Update this README.md: `Source Code Structure` and `Project introduction`

## Source Code Structure

```
.
├── SearchEngineApplication.java
├── controller
│   └── SearchController.java
├── engine
│   ├── KeywordCounterEngine.java
│   └── KeywordExtractionEngine.java
├── model
│   ├── Keyword.java
│   ├── KeywordExtractionResult.java
│   └── Page.java
└── service
    └── GoogleQuery.java
```
