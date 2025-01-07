import requests
import json

# Java API 的 URL
api_url = "http://localhost:8080/api/search"

# 查詢參數
query = "夜市 美食"
params = {"query": query}

try:
    response = requests.get(api_url, params=params)

    if response.status_code == 200:
        result_json = response.json()
        print(json.dumps(result_json, indent=4, ensure_ascii=False))
    else:
        print(f"Error: Received status code {response.status_code}")
        print(response.text)

except requests.exceptions.RequestException as e:
    print(f"Request failed: {e}")
