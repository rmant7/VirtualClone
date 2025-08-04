from flask import Blueprint, request, jsonify
from langdetect import detect, LangDetectException

text_api = Blueprint('text_api', __name__)

@text_api.route('/submit-text', methods=['POST'])
def submit_text():
    data = request.get_json()
    text = data.get("text")

    if not text:
        return jsonify({"error": "No text provided"}), 400

    try:
        language = detect(text)
    except LangDetectException:
        return jsonify({"error": "Could not detect language"}), 400

    # You can also print or log the result
    print(f"Received text: {text}")
    print(f"Detected language: {language}")

    return jsonify({
        "message": "Text received successfully",
        "language": language
    })
