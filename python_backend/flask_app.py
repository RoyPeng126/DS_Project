from flask import Flask, request, jsonify
from ckip_transformers.nlp import CkipWordSegmenter, CkipPosTagger
from contextlib import redirect_stdout, redirect_stderr
import os
import sys
import voyageai

# Initialize Flask app
app = Flask(__name__)

# Initialize CKIP drivers
ws_driver = CkipWordSegmenter(model="albert-base")
pos_driver = CkipPosTagger(model="albert-base")

# Configure VoyageAI API key
voyage_api_key = os.getenv('VOYAGEAI_API_KEY')
if not voyage_api_key:
    raise EnvironmentError("Environment variable 'VOYAGEAI_API_KEY' is not set.")

def clean(sentence_ws, sentence_pos):
    short_sentence = []
    stop_pos = set(["Nep", "Nh", "Nb"])
    for word_ws, word_pos in zip(sentence_ws, sentence_pos):
        is_N_or_V = word_pos.startswith("V") or word_pos.startswith("N")
        is_not_stop_pos = word_pos not in stop_pos
        is_not_one_charactor = not (len(word_ws) == 1)
        if is_N_or_V and is_not_stop_pos and is_not_one_charactor:
            short_sentence.append(f"{word_ws}")
    return " ".join(short_sentence)

def silent_call_ckip_v2(question):
    original_stdout = sys.stdout
    original_stderr = sys.stderr
    try:
        with open(os.devnull, "w") as fnull:
            with redirect_stdout(fnull), redirect_stderr(fnull):
                ws = ws_driver([question])
                pos = pos_driver(ws)
    finally:
        sys.stdout = original_stdout
        sys.stderr = original_stderr
    return ws, pos

def rerank_with_voyage(query, documents, api_key):
    vo = voyageai.Client(api_key=api_key)

    # 調用 VoyageAI API 進行重排序
    reranking = vo.rerank(query, documents, model='rerank-2', top_k=20)

    # 獲取排序結果
    ranked_documents_with_scores = [
        {"document": result.document, "score": result.relevance_score}
        for result in reranking.results
    ]

    return ranked_documents_with_scores

@app.route('/extract_keywords', methods=['POST'])
def extract_keywords():
    data = request.get_json()
    if not data or 'text' not in data:
        return jsonify({"error": "Missing 'text' in request body."}), 400

    question = data['text']

    try:
        ws, pos = silent_call_ckip_v2(question)
        keywords = clean(ws[0], pos[0])
        return jsonify({"keywords": keywords.split()})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/rerank', methods=['POST'])
def rerank():
    data = request.get_json()
    if not data or 'query' not in data or 'documents' not in data:
        return jsonify({"error": "Missing 'query' or 'documents' in request body."}), 400

    query = data['query']
    documents = data['documents']

    try:
        ranked_documents_with_scores = rerank_with_voyage(query, documents, voyage_api_key)
        return jsonify({"ranked_documents": ranked_documents_with_scores})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, threaded=True)
