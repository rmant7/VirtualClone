from flask import render_template, current_app
import subprocess
from app.services.file_service import download_audio_from_url
from app.services.transcribe_service import transcribe_audio
import asyncio
import os

def handle_links(urls, link_type='single'):
    """
    Handles the submission of links for audio transcription.
    Supports both single and batch link submissions.
    """
    try:
        transcript = ''

        if link_type == 'single':
            transcript = handle_single_link(urls[0])
        elif link_type == 'playlist':
            transcript = handle_playlist_link(urls[0])
        elif link_type == 'batch':
            print(f"Processing batch links: {urls}")
            transcript = handle_links_batch_sync(urls)#handle_links_batch(urls)

        return render_template('links.html', transcript=transcript)
    except Exception as e:
        return render_template('links.html', error=str(e))
    

def handle_links_batch(urls):
    try:
        return asyncio.run(handle_links_batch_async(urls))
    except Exception as e:
        raise Exception(str(e))
    
def handle_links_batch_sync(urls):
    results = []
    for url in urls:
        try:
            print(f"Processing: {url}")
            audio_path = download_audio_from_url(url)
            if not os.path.exists(audio_path) or os.path.getsize(audio_path) < 1024:
                results.append(f"[Error: audio file invalid for {url}]")
                continue

            text = transcribe_audio(audio_path)
            if not text:
                results.append(f"[Error: transcription failed for {url}]")
                continue

            results.append(f"--- Transcription for {url} ---\n{text}")
        except Exception as e:
            results.append(f"[Error processing {url}]: {str(e)}")

    return "\n\n".join(results)


def handle_single_link(url):
    try:
        audio_path = download_audio_from_url(url)
        transcript = transcribe_audio(audio_path)
        return transcript
    except Exception as e:
        raise Exception(str(e))
    

def handle_playlist_link(channel_url):
    max_videos = current_app.config['MAX_VIDEOS']

    try:
        command = ["yt-dlp", "--flat-playlist", "--get-id", channel_url]
        if max_videos:
            command += ["--playlist-end", str(max_videos)]

        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
        video_ids = result.stdout.strip().splitlines()

        video_urls = []
        transcript = ''
        for vid in video_ids:
            vid = vid.strip()
            if not vid:
                continue

            url =''
            if vid.startswith("http://") or vid.startswith("https://"):
                video_urls.append(vid)
                url = vid
            else:
                video_urls.append(f"https://www.youtube.com/watch?v={vid}")
                url = f"https://www.youtube.com/watch?v={vid}"

            audio_path = download_audio_from_url(url)
            transcript += '\n' + transcribe_audio(audio_path)
        return transcript
    except Exception as e:
        raise Exception(str(e))
    

async def handle_links_batch_async(urls):
    try:
        tasks = [process_url_async(url) for url in urls]
        results = await asyncio.gather(*tasks)
        print("Batch processing complete.", results)
        labeled_results = [
            f"--- Transcription for {url} ---\n{transcript.strip()}"
            for url, transcript in zip(urls, results)
        ]

        return "\n\n".join(labeled_results)
    except Exception as e:
        raise Exception(str(e))    

async def process_url_async(url):
    try:
        print(f"Start downloading: {url}")
        audio_path = await asyncio.to_thread(download_audio_from_url, url)
        print(f"Downloaded to: {audio_path}")
        transcript = await asyncio.to_thread(transcribe_audio, audio_path)
        print(f"Finished transcription for {url[:50]}: {transcript[:20]}")

        return transcript or f"[No transcription for {url}]"
    except Exception as e:
        return f"[Error processing {url}]: {str(e)}"