from flask import render_template, current_app
from app.services.file_service import save_file, allowed_file
from app.services.transcribe_service import extract_audio, transcribe_audio
import os

def handle_upload(request):
    file = request.files.get('file')
    if not file or file.filename == '':
        return "No file selected", 400

    try:
        if not allowed_file(file.filename):
            return "Invalid file type", 400

        filename = save_file(file)
        video_path = os.path.join(current_app.config['UPLOAD_FOLDER'], filename)

        audio_path = video_path.rsplit('.', 1)[0] + ".wav"
        extract_audio(video_path, audio_path)

        transcript = transcribe_audio(audio_path)
        allowed_exts = current_app.config['ALLOWED_EXTENSIONS']
        return render_template("upload.html", filename=filename, transcript=transcript, allowed_extensions=allowed_exts)
    except Exception as e:
        return f"An error occurred: {str(e)}", 500


