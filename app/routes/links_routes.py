from flask import request, Blueprint, render_template
from app.controllers.links_controller import handle_links

links_bp = Blueprint('links', __name__)

@links_bp.route('/submit-link', methods=['POST', 'GET'])
def submit_link():
    if request.method == 'POST':
        link_type = request.form.get('link_type', 'single')
        urls = []
        if link_type == 'batch':
            urls = request.form.get('batch_links', '').splitlines()
            if not urls:
                return render_template('links.html', error='No URLs provided for batch processing')
        else:
            urls = [request.form.get('input_link')]

        if not urls:
            raise Exception('Missing URL')
    
        try:
            return handle_links(urls, link_type)
        except Exception as e:
            return render_template('links.html', error=str(e))
    
    return render_template('links.html')


