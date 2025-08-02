import os

class Config:
    UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), '..', 'uploads')
    MAX_CONTENT_LENGTH = 30 * 1024 * 1024  # 30MB limit
    ALLOWED_EXTENSIONS = {'mp4', 'mov', 'avi', 'mkv'}