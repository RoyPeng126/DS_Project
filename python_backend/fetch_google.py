from requests_html import HTMLSession
from bs4 import BeautifulSoup

def fetch_google_result_text(query):
    # URL encode query for Google search
    url = f"https://www.google.com/search?q={query}"
    
    # Set up session to mimic a browser
    session = HTMLSession()
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    }
    
    try:
        # Get the response
        response = session.get(url, headers=headers)
        # response.html.render(sleep=2)  # Render JavaScript content if needed

        # Parse the HTML content
        soup = BeautifulSoup(response.html.html, "html.parser")
        
        # Find the unique element with class 'y6Uyqe'
        element = soup.find(class_="y6Uyqe")
        if not element:
            return []  # Return empty list if not found
        
        # Extract all text content recursively
        def extract_text_recursively(tag):
            text_list = []
            for child in tag.descendants:
                if child.name is None:  # It's a text node
                    cleaned_text = child.strip()
                    if cleaned_text:  # Ignore empty strings
                        text_list.append(cleaned_text)
            return text_list
        
        # Process the found element
        return extract_text_recursively(element)
    
    except Exception as e:
        print(f"Error occurred: {e}")
        return []


if __name__ == "__main__":
    # Example usage
    query = "香腸 台北"
    result_list = fetch_google_result_text(query)
    print(result_list)
