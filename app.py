from flask import Flask, request, jsonify
from flasgger import Swagger
from utils import download_audio_from_url, transcribe_audio_file  # Use transcribe_audio_file here

app = Flask(__name__)
swagger = Swagger(app)

@app.route('/submit-link', methods=['POST'])
def submit_link():
    data = request.get_json()
    if not data or 'url' not in data:
        return jsonify({'error': 'Missing URL'}), 400

    url = data['url']
    try:
        audio_path = download_audio_from_url(url)
        transcript = transcribe_audio_file(audio_path)  # Call the correct function
        return jsonify({'transcript': transcript}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=5001)
