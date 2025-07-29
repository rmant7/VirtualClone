import os
import yt_dlp
import ssl
import certifi

ssl._create_default_https_context = ssl.create_default_context(cafile=certifi.where())

ssl._create_default_https_context = ssl._create_unverified_context
import ssl
import certifi

ssl._create_default_https_context = ssl.create_default_context(cafile=certifi.where())

import ssl
ssl._create_default_https_context = ssl._create_unverified_context

def download_youtube_audio(url, output_dir='downloads'):
    os.makedirs(output_dir, exist_ok=True)

    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': f'{output_dir}/%(title)s.%(ext)s',
        'postprocessors': [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }],
        'quiet': False
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)
        # Replace audio extension with .mp3 since we extract audio
        filename = os.path.splitext(filename)[0] + '.mp3'
    return filename


if __name__ == "__main__":
    test_url = input("Enter YouTube URL: ")
    audio_file = download_youtube_audio(test_url)
    print(f"Audio downloaded to: {audio_file}")

