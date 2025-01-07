# Final Project - Let's Beat Google!

## Topic

Snack Hunter: The Ultimate Taiwanese Night Market Food Finder

## Project introduction

[Project Final PDF](./DSproposal_Group9_final.pdf)

## Repo Structure

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

## Setup Environment
To set up the development environment, follow these steps:

- Please install it yourself in advance: python / pip / pyvenv / java / maven / springboot

1. git clone our repo:
    ```
    git clone https://github.com/RoyPeng126/DS_Project
    cd DS_Project
    ```

2. setup python environment:
    ```
    cd python_backend

    python -m venv ds_venv
    source ds_venv/bin/activate

    pip install -r requirements.txt
    ```

3. setup environment variable (Please go to the voyage and gcp official websites to obtain the Keys):
    ```c
    // windows
    set VOYAGEAI_API_KEY=
    set GOOGLE_CLOUD_API_KEY=
    set GOOGLE_CLOUD_SEARCH_ENGINE_ID=

    // linux / mac
    export VOYAGEAI_API_KEY=
    export GOOGLE_CLOUD_API_KEY=
    export GOOGLE_CLOUD_SEARCH_ENGINE_ID=
    ```

4. run python service:
    ```
    python flask_app.py
    ```

5. setup java environment:
    ```c
    // open a new terminal
    cd Java_MainBackend
    mvn clean install
    ```

6. setup environment variable in new terminal:
    ```c
    // windows
    set VOYAGEAI_API_KEY=
    set GOOGLE_CLOUD_API_KEY=
    set GOOGLE_CLOUD_SEARCH_ENGINE_ID=

    // linux / mac
    export VOYAGEAI_API_KEY=
    export GOOGLE_CLOUD_API_KEY=
    export GOOGLE_CLOUD_SEARCH_ENGINE_ID=
    ```

7. run java service:
    ```
    mvn spring-boot:run
    ```

8. Open Web Version:
    - Launch browser: [http://localhost:8080](http://localhost:8080)

9. iOS App Version:

    - First, use a Mac computer to download Xcode.
    - Open the `NightMarketSearch` folder located in the `./IOSAPP_Source` directory of the repo in Xcode.
    - Run the app (using an iOS 16 or later simulator).
    - Alternatively, you can export the app directly to your personal iPhone:
        - In this case, ensure that the backend is fully deployed to a server beforehand, and modify the Swift code to replace `localhost` with the appropriate domain/IP address.
        - Additionally, you will need to set your iPhone to "trust" the app.
