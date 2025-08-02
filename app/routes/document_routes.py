from flask import Blueprint, render_template, request, current_app
from app.controllers.document_controller import handle_upload

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/', methods=['GET', 'POST'])
def upload():
    if request.method == 'POST':
        return handle_upload(request)
    allowed_exts = current_app.config['ALLOWED_EXTENSIONS']
    return render_template('upload.html', allowed_extensions=allowed_exts)
