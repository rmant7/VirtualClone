from sqlalchemy import text
import whisper
import os
import subprocess
import json

def extract_audio(video_path, audio_path):
    try:
        command = [
            "ffmpeg", "-i", video_path,
            "-vn",  # no video
            "-acodec", "pcm_s16le",  # WAV format
            "-ar", "16000",          # 16kHz sample rate
            "-ac", "1",              # mono audio
            audio_path
        ]

        subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
    except Exception as e:
        print(f"Error extracting audio: {e}")

def transcribe_audio(audio_path):
    try:
        model = whisper.load_model('base')
        result = model.transcribe(audio_path)
        text = result['text']

        BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
        DATA_DIR = os.path.join(BASE_DIR, "data")

        os.makedirs(DATA_DIR, exist_ok=True)
        with open(os.path.join(DATA_DIR, "train.jsonl"), "a", encoding="utf-8") as f:
            json.dump({"text": text.strip()}, f, ensure_ascii=False)
            f.write("\n")

        return text
    except Exception as e:
        print(f"Error transcribing audio: {e}")
        return None

