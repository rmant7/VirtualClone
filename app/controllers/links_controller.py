from flask import render_template, current_app
import subprocess
import yt_dlp
import requests
import os
import re
import logging

logger = logging.getLogger(__name__)

def get_groq_key():
    from dotenv import load_dotenv
    load_dotenv()
    return os.environ.get('GROQ_API_KEY', '')

def extract_video_id(url):
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
    ydl_opts = {
    'format': 'bestaudio[ext=m4a]/bestaudio/best',
    'quiet': False,
    'skip_download': True,
    'extractor_args': {
        'youtube': {
            'player_client': ['web'],
        }
    },
    'postprocessors': [],
    'cookiefile': os.path.join(os.path.dirname(__file__), '../../cookies.txt'),
    'js_runtimes': {'node': {}},
    'remote_components': {'ejs': 'github'},
}

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
            if not info:
                return None, None

            title = info.get('title', 'Unknown')
            formats = info.get('formats', [])

            audio_formats = [
                f for f in formats
                if f.get('acodec') != 'none'
                and f.get('vcodec') == 'none'
                and f.get('url')
            ]

            if audio_formats:
                audio_formats.sort(key=lambda x: x.get('abr') or 0, reverse=True)
                return audio_formats[0]['url'], title

            for fmt in reversed(formats):
                if fmt.get('url') and fmt.get('acodec') != 'none':
                    return fmt['url'], title

            if info.get('url'):
                return info['url'], title

    except Exception as e:
        logger.error(f"yt-dlp error: {e}")
        print(f"YT-DLP FULL ERROR: {e}")

    return None, None
def transcribe_on_the_fly(audio_url, title='audio'):
    """Stream audio directly to Groq Whisper - no file saved"""
    try:
        GROQ_API_KEY = get_groq_key()
        if not GROQ_API_KEY:
            raise Exception("GROQ_API_KEY not configured in .env")

        # Stream audio to memory
        headers = {
            'User-Agent': 'Mozilla/5.0',
            'Range': 'bytes=0-'
        }

        logger.info(f"Streaming audio for: {title}")
        audio_response = requests.get(
            audio_url,
            headers=headers,
            stream=True,
            timeout=60
        )

        if not audio_response.ok:
            raise Exception(f"Failed to stream audio: {audio_response.status_code}")

        audio_content = audio_response.content
        logger.info(f"Audio size: {len(audio_content)} bytes")

        # Send to Groq Whisper API
        groq_url = "https://api.groq.com/openai/v1/audio/transcriptions"

        files = {
            'file': (f'{title}.m4a', audio_content, 'audio/m4a')
        }
        data = {
            'model': 'whisper-large-v3',
            'response_format': 'json'
        }
        groq_headers = {
            'Authorization': f'Bearer {GROQ_API_KEY}'
        }

        logger.info("Sending to Groq Whisper API...")
        response = requests.post(
            groq_url,
            files=files,
            data=data,
            headers=groq_headers,
            timeout=120
        )

        if response.ok:
            result = response.json()
            text = result.get('text', '')
            logger.info(f"Transcription successful: {len(text)} chars")
            return text
        else:
            raise Exception(f"Groq API error {response.status_code}: {response.text}")

    except Exception as e:
        logger.error(f"Transcription error: {e}")
        raise

def handle_links(urls, link_type='single'):
    """Handle link submissions - on the fly, no downloading"""
    try:
        transcript = ''

        if link_type == 'single':
            transcript = handle_single_link(urls[0])
        elif link_type == 'playlist':
            transcript = handle_playlist_link(urls[0])
        elif link_type == 'batch':
            logger.info(f"Processing batch: {len(urls)} URLs")
            transcript = handle_links_batch_sync(urls)

        return render_template('links.html', transcript=transcript)

    except Exception as e:
        logger.error(f"Error handling links: {e}")
        return render_template('links.html', error=str(e))

def handle_single_link(url):
    """Process single link - on the fly"""
    logger.info(f"Processing single link: {url}")

    audio_url, title = get_audio_url_on_the_fly(url)
    if not audio_url:
        raise Exception("Could not extract audio URL from YouTube")

    logger.info(f"Got audio URL for: {title}")
    transcript = transcribe_on_the_fly(audio_url, title)
    return f"--- {title} ---\n\n{transcript}"

def handle_links_batch_sync(urls):
    """Process multiple URLs - on the fly, no downloading"""
    results = []

    for idx, url in enumerate(urls, 1):
        url = url.strip()
        if not url:
            continue

        try:
            logger.info(f"Processing {idx}/{len(urls)}: {url}")

            audio_url, title = get_audio_url_on_the_fly(url)
            if not audio_url:
                results.append(f"[Error: Could not get audio for {url}]")
                continue

            text = transcribe_on_the_fly(audio_url, title)
            results.append(f"--- {title} ---\n\n{text}")
            logger.info(f"Successfully processed: {title}")

        except Exception as e:
            logger.error(f"Error processing {url}: {e}")
            results.append(f"[Error processing {url}]: {str(e)}")

    return "\n\n".join(results)

def handle_playlist_link(channel_url):
    """Process playlist - on the fly"""
    max_videos = current_app.config.get('MAX_VIDEOS', 5)

    try:
        logger.info(f"Processing playlist: {channel_url}")

        command = [
            "yt-dlp",
            "--flat-playlist",
            "--get-id",
            "--playlist-end", str(max_videos),
            channel_url
        ]

        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=30
        )

        video_ids = result.stdout.strip().splitlines()
        logger.info(f"Found {len(video_ids)} videos")

        transcripts = []
        for idx, vid in enumerate(video_ids, 1):
            vid = vid.strip()
            if not vid:
                continue

            url = vid if vid.startswith("http") else \
                f"https://www.youtube.com/watch?v={vid}"

            try:
                logger.info(f"Processing {idx}/{len(video_ids)}: {url}")
                audio_url, title = get_audio_url_on_the_fly(url)

                if not audio_url:
                    transcripts.append(f"[Error: no audio for {url}]")
                    continue

                text = transcribe_on_the_fly(audio_url, title)
                transcripts.append(f"--- {title} ---\n\n{text}")

            except Exception as e:
                logger.error(f"Error: {e}")
                transcripts.append(f"[Error: {str(e)}]")

        return '\n\n'.join(transcripts)

    except Exception as e:
        logger.error(f"Playlist error: {e}")
        raise Exception(str(e))
