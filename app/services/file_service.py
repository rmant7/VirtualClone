import os
from unittest import result
from werkzeug.utils import secure_filename
from flask import current_app
import uuid
import subprocess
import glob
import re

def allowed_file(filename):
    allowed = current_app.config['ALLOWED_EXTENSIONS']
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in allowed

def save_file(file):
    upload_folder = current_app.config['UPLOAD_FOLDER']
    os.makedirs(upload_folder, exist_ok=True)

    original_name = secure_filename(file.filename)
    name, ext = os.path.splitext(original_name)
    unique_suffix = uuid.uuid4().hex[:8]  # short unique ID
    unique_filename = f"{name}_{unique_suffix}{ext}"
    filepath = os.path.join(upload_folder, unique_filename)
    
    file.save(filepath)
    
    return unique_filename

def download_audio_from_url(url):
    output_dir = "./downloads"
    os.makedirs(output_dir, exist_ok=True)

    title = get_video_title(url)
    safe_title = sanitize_title(title)
    output_filename = f"{safe_title}.mp3"
    output_path = os.path.join(output_dir, output_filename)

    output_template = os.path.join(output_dir, f"{safe_title}.%(ext)s")
    command = [
        "yt-dlp",
        "-x",
        "--audio-format", "mp3",
        "-o", output_template,
        url
    ]

    try:
        subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)        

        if not os.path.exists(output_path):
            raise Exception(f"Expected file not found: {output_path}")

        return output_path
    except subprocess.CalledProcessError as e:
        raise Exception(f"Download failed: {e.stderr.strip()}")
    


def get_video_title(url):
    try:
        result = subprocess.run(
            ["yt-dlp", "--get-title", url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        title = result.stdout.strip()
        return title
    except subprocess.CalledProcessError as e:
        raise Exception(f"Failed to get title: {e.stderr.strip()}")


def sanitize_title(title):
    # Remove/replace characters that are invalid in filenames
    return re.sub(r'[\\/*?:"<>|]', "_", title).strip()