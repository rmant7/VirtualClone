from flask import Blueprint, request, jsonify
from yt_dlp import YoutubeDL
import yt_dlp
import os
import certifi


os.environ['SSL_CERT_FILE'] = certifi.where()


link_api = Blueprint('link_api', __name__)

@link_api.route('/submit-link', methods=['POST'])
def submit_link():
    data = request.get_json()
    if not data or 'link' not in data:
        return jsonify({"error": "No link provided"}), 400

    video_url = data['link']
    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': 'downloads/%(id)s.%(ext)s',

    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(video_url, download=True)
            video_title = info.get('title', 'Unknown title')
    except Exception as e:
        return jsonify({"error": str(e)}), 500

    return jsonify({"message": "Link processed successfully", "title": video_title}), 200
