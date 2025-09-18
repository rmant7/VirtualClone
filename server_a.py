from flask import Flask, request, jsonify
import speech_recognition as sr
import os

app = Flask(__name__)

UPLOAD_FOLDER = '/home/isra1elmugrabi/uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/transcribe', methods=['POST'])
def transcribe_audio():
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'No file provided'}), 400

        audio_file = request.files['file']
        save_path = os.path.join(UPLOAD_FOLDER, audio_file.filename)
        audio_file.save(save_path)

        recognizer = sr.Recognizer()
        with sr.AudioFile(save_path) as source:
            audio_data = recognizer.record(source)
            text = recognizer.recognize_google(audio_data)

        return jsonify({'message': 'Server working', 'transcription': text})

    except Exception as e:
        return jsonify({'error': str(e)}), 500
