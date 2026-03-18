from flask import Blueprint, request, jsonify, render_template
import yt_dlp
import requests
import tempfile
import os
import re

whisper_links = Blueprint('whisper_links', __name__)

GROQ_API_KEY = os.environ.get('GROQ_API_KEY', '')

def extract_video_id(url):
    """Extract YouTube video ID from URL"""
    patterns = [
        r'(?:youtube\.com/watch\?v=|youtu\.be/)([a-zA-Z0-9_-]{11})',
        r'youtube\.com/embed/([a-zA-Z0-9_-]{11})',
        r'youtube\.com/shorts/([a-zA-Z0-9_-]{11})'
    ]
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    return None

def get_audio_url_on_the_fly(youtube_url):
    """Get audio stream URL WITHOUT downloading - on the fly"""
    ydl_opts = {
        'format': 'bestaudio[ext=m4a]/bestaudio/best',
        'quiet': True,
        'no_warnings': False,
        'skip_download': True,
        # Add Node.js as JS runtime
        'extractor_args': {
            'youtube': {
                'player_client': ['web'],
                'js_runtimes': ['node']
            }
        },
        # Skip ffmpeg postprocessing since we just need URL
        'postprocessors': [],
    }
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
            
            if not info:
                return None, None
                
            title = info.get('title', 'Unknown')
            
            # Get direct audio URL
            formats = info.get('formats', [])
            
            # Find best audio-only format
            audio_formats = [
                f for f in formats 
                if f.get('acodec') != 'none' 
                and f.get('vcodec') == 'none'
                and f.get('url')
            ]
            
            if audio_formats:
                # Sort by bitrate, get best
                audio_formats.sort(key=lambda x: x.get('abr', 0), reverse=True)
                return audio_formats[0]['url'], title
            
            # Fallback: any format with audio
            for fmt in reversed(formats):
                if fmt.get('url') and fmt.get('acodec') != 'none':
                    return fmt['url'], title
                    
            # Last resort: direct URL
            if info.get('url'):
                return info['url'], title
                
    except Exception as e:
        print(f"yt-dlp error: {e}")
        
    return None, None
def transcribe_audio_url(audio_url, video_title):
    """Stream audio URL directly to Groq Whisper - no file download"""
    try:
        # Stream audio content on the fly
        headers = {
            'User-Agent': 'Mozilla/5.0',
            'Range': 'bytes=0-'
        }
        
        # Download audio content to memory (not disk)
        audio_response = requests.get(audio_url, headers=headers, 
                                       stream=True, timeout=60)
        
        if not audio_response.ok:
            return None, f"Failed to stream audio: {audio_response.status_code}"
        
        # Get audio content
        audio_content = audio_response.content
        
        # Send directly to Groq Whisper API
        groq_url = "https://api.groq.com/openai/v1/audio/transcriptions"
        
        files = {
            'file': (f'{video_title}.m4a', audio_content, 'audio/m4a')
        }
        data = {
            'model': 'whisper-large-v3',
            'response_format': 'json'
        }
        headers_groq = {
            'Authorization': f'Bearer {GROQ_API_KEY}'
        }
        
        response = requests.post(groq_url, files=files, 
                                  data=data, headers=headers_groq,
                                  timeout=120)
        
        if response.ok:
            result = response.json()
            return result.get('text', ''), None
        else:
            return None, f"Groq API error: {response.status_code} - {response.text}"
            
    except Exception as e:
        return None, str(e)

@whisper_links.route('/links/submit-link', methods=['GET'])
def submit_link_page():
    """Render the link submission page"""
    return render_template('whisper_links.html')

@whisper_links.route('/links/transcribe', methods=['POST'])
def transcribe_links():
    """Transcribe multiple YouTube links on the fly"""
    data = request.get_json()
    
    if not data:
        return jsonify({'error': 'No data provided'}), 400
    
    urls = data.get('urls', [])
    
    if not urls:
        return jsonify({'error': 'No URLs provided'}), 400
    
    if not GROQ_API_KEY:
        return jsonify({'error': 'GROQ_API_KEY not configured'}), 500
    
    results = []
    
    for url in urls:
        url = url.strip()
        if not url:
            continue
            
        video_id = extract_video_id(url)
        if not video_id:
            results.append({
                'url': url,
                'success': False,
                'error': 'Invalid YouTube URL'
            })
            continue
        
        try:
            # Get audio URL on the fly - no download
            audio_url, title = get_audio_url_on_the_fly(url)
            
            if not audio_url:
                results.append({
                    'url': url,
                    'video_id': video_id,
                    'success': False,
                    'error': 'Could not extract audio URL'
                })
                continue
            
            # Transcribe directly from stream
            transcription, error = transcribe_audio_url(audio_url, title or video_id)
            
            if transcription:
                results.append({
                    'url': url,
                    'video_id': video_id,
                    'title': title,
                    'success': True,
                    'transcription': transcription
                })
            else:
                results.append({
                    'url': url,
                    'video_id': video_id,
                    'success': False,
                    'error': error or 'Transcription failed'
                })
                
        except Exception as e:
            results.append({
                'url': url,
                'video_id': video_id,
                'success': False,
                'error': str(e)
            })
    
    return jsonify({
        'results': results,
        'total': len(results),
        'successful': sum(1 for r in results if r['success'])
    })