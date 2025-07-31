from flask import render_template
from app.services.file_service import save_file, allowed_file

def handle_upload(request):
    file = request.files.get('file')
    if not file or file.filename == '':
        return "No file selected", 400

    if not allowed_file(file.filename):
        return "Invalid file type", 400

    filename = save_file(file)
    return render_template("upload.html", filename=filename)
