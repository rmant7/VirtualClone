import os
from werkzeug.utils import secure_filename
from flask import current_app
import uuid

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