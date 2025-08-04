import os
import subprocess
from audio_transcriber import transcribe_audio

def transcribe_audio_file(audio_path):
    return transcribe_audio(audio_path)

def download_audio_from_url(url):
    output_dir = "./downloads"
    os.makedirs(output_dir, exist_ok=True)

    command = [
        "yt-dlp",
        "-x",
        "--audio-format", "mp3",
        "-o", f"{output_dir}/%(title)s.%(ext)s",
        url
    ]

    subprocess.run(command, check=True)

    files = sorted(os.listdir(output_dir), key=lambda f: os.path.getmtime(os.path.join(output_dir, f)), reverse=True)
    if not files:
        raise Exception("No audio file downloaded.")

    latest_file = os.path.join(output_dir, files[0])
    return latest_file


def transcribe_audio_file(audio_path):  # ✅ Correct naming and spacing
    return transcribe_audio(audio_path)
